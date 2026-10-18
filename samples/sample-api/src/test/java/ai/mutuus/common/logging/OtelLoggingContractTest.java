package ai.mutuus.common.logging;

import ai.mutuus.common.core.VerifiedIdentityContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class OtelLoggingContractTest {
    private static final String TRACE = "1234567890abcdef1234567890abcdef", SPAN = "1234567890abcdef";
    private final CommonJsonEncoder encoder = new CommonJsonEncoder();
    private final ListAppender<ILoggingEvent> collector = new ListAppender<>();
    private final InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();
    private SdkLoggerProvider provider;
    private SnapshotAppender snapshot;
    private Logger logger;
    private Level previousLevel;
    private boolean previousAdditivity;

    @BeforeEach void setup() {
        GlobalOpenTelemetry.resetForTest();
        provider = SdkLoggerProvider.builder().addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build();
        GlobalOpenTelemetry.set(OpenTelemetrySdk.builder().setLoggerProvider(provider).build());
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger = context.getLogger(AccessLogger.LOGGER_NAME);
        previousLevel = logger.getLevel(); previousAdditivity = logger.isAdditive();
        logger.setLevel(Level.INFO); logger.setAdditive(false);
        collector.setContext(context); collector.start();
        snapshot = new SnapshotAppender(); snapshot.setContext(context); snapshot.addAppender(collector); snapshot.start();
        logger.addAppender(snapshot);
    }
    @AfterEach void cleanup() {
        logger.detachAppender(snapshot); snapshot.stop();
        logger.setLevel(previousLevel); logger.setAdditive(previousAdditivity);
        MDC.clear(); provider.close(); GlobalOpenTelemetry.resetForTest();
    }
    private Scope span() {
        return Span.wrap(SpanContext.create(TRACE, SPAN, TraceFlags.getSampled(), TraceState.getDefault())).makeCurrent();
    }
    private Scope identity() {
        return VerifiedIdentityContext.makeCurrent(new VerifiedIdentityContext.Identity("member-42", "member", "authenticated"));
    }
    @Test void immutableSnapshotAndSameEventIdReachFileAndOtlpAfterScopeCleanup() throws Exception {
        try (Scope traced = span(); Scope user = identity()) {
            MDC.put("X-User-Id", "forged"); MDC.put("authorization", "secret");
            logger.atInfo().addKeyValue("user.id", "forged-kv").addKeyValue("unregistered.field", 999).log("hello");
            logger.info("second");
            logger.debug("disabled");
        }
        MDC.clear();
        assertThat(collector.list).hasSize(2);
        Map<String, String> first = fields(collector.list.getFirst());
        assertThat(first).containsEntry("trace.id", TRACE).containsEntry("span.id", SPAN)
                .containsEntry("user.id", "member-42").containsEntry("golmok.identity.realm", "member")
                .containsEntry("log.severity_number", "9");
        assertThat(first.get("event.id")).isNotEqualTo(fields(collector.list.get(1)).get("event.id"));
        assertThat(encoder.encode(collector.list.getFirst())).isEqualTo(encoder.encode(collector.list.getFirst()));
        assertThat(new String(encoder.encode(collector.list.getFirst()), StandardCharsets.UTF_8))
                .doesNotContain("forged", "secret", "unregistered.field", "X-User", "transaction", "logger_name", "level_value");
        var exported = exporter.getFinishedLogRecordItems();
        assertThat(exported).hasSize(2);
        assertThat(exported.getFirst().getAttributes().get(AttributeKey.stringKey("event.id"))).isEqualTo(first.get("event.id"));
        assertThat(exported.getFirst().getAttributes().get(AttributeKey.stringKey("user.id"))).isEqualTo("member-42");
        assertThat(exported.getFirst().getSpanContext().getTraceId()).isEqualTo(TRACE);
        assertThat(exported.getFirst().getSpanContext().getSpanId()).isEqualTo(SPAN);
        logger.info("unrelated");
        assertThat(fields(collector.list.getLast())).doesNotContainKeys("user.id", "trace.id", "span.id");
    }
    @Test void authenticated403SnapshotSurvivesInnerScopeCleanup() throws Exception {
        var request = request(); var response = new MockHttpServletResponse();
        try (Scope traced = span()) {
            filter().doFilter(request, response, (req, res) -> {
                try (Scope user = identity()) {
                    req.setAttribute(VerifiedIdentityContext.REQUEST_ATTRIBUTE, VerifiedIdentityContext.current());
                    response.setStatus(403);
                }
            });
        }
        assertThat(collector.list).hasSize(1);
        assertThat(fields(collector.list.getFirst())).containsEntry("event.action", "http.server.complete")
                .containsEntry("http.response.status_code", "403").containsEntry("user.id", "member-42")
                .containsEntry("event.outcome", "failure").containsEntry("url.path", "/members/{id}");
    }
    @Test void realAsyncAppenderEncodesAfterIdentityScopeEnds() throws Exception {
        var release = new java.util.concurrent.CountDownLatch(1);
        var finished = new java.util.concurrent.CountDownLatch(1);
        var encoded = new java.util.concurrent.atomic.AtomicReference<byte[]>();
        var delayed = new ch.qos.logback.core.AppenderBase<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) {
                try {
                    if (release.await(5, java.util.concurrent.TimeUnit.SECONDS)) encoded.set(encoder.encode(event));
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                finally { finished.countDown(); }
            }
        };
        delayed.setContext(logger.getLoggerContext()); delayed.start();
        var async = new ch.qos.logback.classic.AsyncAppender();
        async.setContext(logger.getLoggerContext()); async.addAppender(delayed); async.start();
        snapshot.addAppender(async);
        try {
            try (Scope traced = span(); Scope user = identity()) { logger.info("queued"); }
            release.countDown();
            assertThat(finished.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(encoded.get()).isEqualTo(encoder.encode(collector.list.getFirst()));
        } finally { release.countDown(); snapshot.detachAppender(async); async.stop(); delayed.stop(); }
    }
    @Test void asyncCompleteUsesCapturedContextAndEmitsOnlyOnce() throws Exception { asyncTerminal("complete", 200); }
    @Test void unauthenticated401HasFailedStateAndNoForgedUser() throws Exception {
        var request = request(); request.addHeader("X-User-Id", "forged"); var response = new MockHttpServletResponse();
        try (Scope traced = span()) {
            filter().doFilter(request, response, (req, res) -> response.setStatus(401));
        }
        assertThat(collector.list).hasSize(1);
        assertThat(fields(collector.list.getFirst())).containsEntry("http.response.status_code", "401")
                .containsEntry("golmok.auth.state", "failed").doesNotContainKey("user.id");
    }
    @Test void asyncTimeoutUsesCapturedContextAndEmitsOnlyOnce() throws Exception { asyncTerminal("timeout", 504); }
    @Test void asyncErrorUsesCapturedContextAndEmitsOnlyOnce() throws Exception { asyncTerminal("error", 500); }
    private void asyncTerminal(String terminal, int status) throws Exception {
        var request = request(); request.setAsyncSupported(true); var response = new MockHttpServletResponse();
        try (Scope traced = span()) {
            filter().doFilter(request, response, (req, res) -> {
                try (Scope user = identity()) {
                    req.setAttribute(VerifiedIdentityContext.REQUEST_ATTRIBUTE, VerifiedIdentityContext.current());
                    req.startAsync();
                }
            });
        }
        assertThat(collector.list).isEmpty();
        MockAsyncContext context = (MockAsyncContext) request.getAsyncContext();
        AsyncEvent event = new AsyncEvent(context, request, response, new IOException("test failure"));
        for (var listener : context.getListeners()) {
            if (terminal.equals("timeout")) listener.onTimeout(event);
            else if (terminal.equals("error")) listener.onError(event);
            else listener.onComplete(event);
            listener.onComplete(event);
        }
        assertThat(collector.list).hasSize(1);
        assertThat(fields(collector.list.getFirst())).containsEntry("event.action", "http.server.complete")
                .containsEntry("http.response.status_code", Integer.toString(status)).containsEntry("trace.id", TRACE)
                .containsEntry("span.id", SPAN).containsEntry("user.id", "member-42");
        assertThat(exporter.getFinishedLogRecordItems()).hasSize(1);
        assertThat(exporter.getFinishedLogRecordItems().getFirst().getSpanContext().getSpanId()).isEqualTo(SPAN);
    }
    @Test void syncExceptionStillEmitsOneFailureWithoutFakeAnonymousUser() throws Exception {
        var request = request(); var response = new MockHttpServletResponse();
        try (Scope traced = span()) {
            assertThatThrownBy(() -> filter().doFilter(request, response, (req, res) -> {
                throw new ServletException("failure");
            })).isInstanceOf(ServletException.class);
        }
        assertThat(collector.list).hasSize(1);
        assertThat(fields(collector.list.getFirst())).containsEntry("http.response.status_code", "500")
                .containsEntry("golmok.auth.state", "anonymous").doesNotContainKey("user.id");
    }
    @Test void uniqueCommonFieldsSurviveWithoutAliasDuplication() throws Exception {
        var members = new java.util.ArrayList<>(java.util.List.of("target-1"));
        var headers = new LinkedHashMap<String, String>(); headers.put("content-type", "application/json");
        var previousContext = ai.mutuus.common.core.TraceContext.snapshot();
        ai.mutuus.common.core.TraceContext.put(ai.mutuus.common.core.RequestIds.CONTEXT_KEY, "request-42");
        try (Scope traced = span(); Scope user = identity()) {
            logger.atInfo().addKeyValue("event.dataset", "mutuus.security_audit")
                    .addKeyValue("event.action", "security.identity.mismatch")
                    .addKeyValue("golmok.security.reason", "rejected").addKeyValue("golmok.security.principal", "audit-principal")
                    .addKeyValue("golmok.security.claimed_user_id", "***tail").addKeyValue("golmok.security.authenticated_user_id", "verified-42")
                    .addKeyValue("golmok.security.roles_claim_path", "roles").addKeyValue("golmok.security.target_host", "internal.example")
                    .addKeyValue("golmok.security.dropped_headers", "X-User-Id").addKeyValue("golmok.security.subject", "subject-id")
                    .addKeyValue("golmok.security.event", true).addKeyValue("mutuus.security.disposition", "suspected")
                    .addKeyValue("client.ip", "192.168.1.42").addKeyValue("golmok.actor.id", "actor-42")
                    .addKeyValue("golmok.related_member.ids", members).addKeyValue("golmok.diagnostic.code", "DIAG")
                    .addKeyValue("job.name", "sync")
                    .addKeyValue("code.namespace", "example.Controller").addKeyValue("code.function", "method")
                    .addKeyValue("rule.name", "rule-1").addKeyValue("http.request.mime_type", "application/json")
                    .addKeyValue("http.response.mime_type", "application/json")
                    .addKeyValue("http.request.body.content", "MASKED").addKeyValue("http.request.body.bytes", 6)
                    .addKeyValue("http.response.body.content", "MASKED").addKeyValue("http.response.body.bytes", 6)
                    .addKeyValue("http.request.headers", headers).addKeyValue("mutuus.payload.truncated", true)
                    .addKeyValue("mutuus.method.args", "MASKED_ARGS").addKeyValue("mutuus.method.return", "MASKED_RETURN")
                    .addKeyValue("mutuus.domain_event.id", "domain-42").addKeyValue("mutuus.domain_event.type", "Created")
                    .addKeyValue("url.query", "safe=1").addKeyValue("url.full", "https://internal.example/safe")
                    .addKeyValue("url.domain", "internal.example").log("metadata");
        }
        members.add("late-mutation"); headers.put("late-header", "MUST_NOT_APPEAR");
        Map<String, String> metadata = fields(collector.list.getFirst());
        assertThat(metadata).containsEntry("golmok.security.reason", "rejected")
                .containsEntry("golmok.security.principal", "audit-principal")
                .containsEntry("golmok.security.claimed_user_id", "***tail")
                .containsEntry("golmok.security.authenticated_user_id", "verified-42")
                .containsEntry("golmok.security.roles_claim_path", "roles")
                .containsEntry("golmok.actor.id", "actor-42").containsEntry("golmok.request.id", "request-42")
                .containsEntry("golmok.diagnostic.code", "DIAG").containsEntry("job.name", "sync")
                .containsEntry("code.namespace", "example.Controller").containsEntry("code.function", "method")
                .containsEntry("rule.name", "rule-1").containsEntry("http.request.body.content", "MASKED")
                .containsEntry("http.request.body.bytes", "6").containsEntry("http.request.headers.content-type", "application/json")
                .containsEntry("mutuus.payload.truncated", "true").containsEntry("mutuus.method.args", "MASKED_ARGS")
                .containsEntry("mutuus.domain_event.id", "domain-42").containsEntry("client.ip", "192.168.1.0")
                .containsEntry("user.id", "member-42").doesNotContainKeys("actorId", "requestId", "reason", "X-Event-Id");
        assertThat(new String(encoder.encode(collector.list.getFirst()), StandardCharsets.UTF_8))
                .doesNotContain("late-mutation", "MUST_NOT_APPEAR");
        assertThat(exporter.getFinishedLogRecordItems().getFirst().getAttributes()
                .get(AttributeKey.stringKey("http.request.headers.content-type"))).isEqualTo("application/json");

        var request = request(); request.addHeader("X-Screen-Id", "CATALOG_SCREEN"); request.addHeader("X-Event-Id", "SAVE");
        request.addHeader("X-Device-Level", "ANDROID"); request.addHeader("X-Locale", "ko-KR");
        request.addHeader("X-Screen-Catalog-Version", "2026-10-18.1"); request.addHeader("X-Business-Screen-No", "APP-001");
        request.addHeader("X-Internal-Screen-Id", "GM-SCR-000001");
        try (Scope traced = span()) { filter().doFilter(request, new MockHttpServletResponse(), (req, res) -> { }); }
        Map<String, String> completion = fields(collector.list.getLast());
        assertThat(completion).containsEntry("golmok.screen.id", "CATALOG_SCREEN").containsEntry("golmok.ui.event.id", "SAVE")
                .containsEntry("golmok.device.level", "ANDROID").containsEntry("golmok.locale", "ko-KR")
                .containsEntry("golmok.screen.catalog_version", "2026-10-18.1")
                .containsEntry("golmok.screen.business_no", "APP-001").containsEntry("golmok.screen.internal_id", "GM-SCR-000001");
        assertThat(completion.get("event.id")).isNotEqualTo(completion.get("golmok.request.id")).isNotEqualTo("SAVE");
    }
    @Test void executionScopeAndApprovedResourceMetadataArePreserved() throws Exception {
        Map<String, String> environment = Map.of("OTEL_SERVICE_NAME", "golmok-batch", "OTEL_RESOURCE_ATTRIBUTES",
                "service.name=env-fallback,service.instance.id=instance-42,service.version=0.4.0-rc.1,deployment.environment.name=dev,secret.token=MUST_NOT_APPEAR");
        var resource = CommonLogResource.fields(Map.<String, String>of()::get, environment::get);
        assertThat(resource).containsEntry("service.name", "golmok-batch").containsEntry("service.instance.id", "instance-42")
                .containsEntry("service.version", "0.4.0-rc.1").containsEntry("deployment.environment.name", "development")
                .doesNotContainKey("secret.token");
        var overridden = CommonLogResource.fields(Map.of("otel.service.name", "approved-override",
                "otel.resource.attributes", "service.version=0.4.0,deployment.environment.name=stage")::get, environment::get);
        assertThat(overridden).containsEntry("service.name", "approved-override").containsEntry("service.version", "0.4.0")
                .containsEntry("deployment.environment.name", "staging");
        try (Scope traced = span(); Scope job = ai.mutuus.common.core.TelemetryScope
                .with(io.opentelemetry.context.Context.current(), "job").makeCurrent()) {
            logger.info("ordinary job line");
        }
        logger.info("after job");
        assertThat(fields(collector.list.getFirst())).containsEntry("golmok.scope", "job");
        assertThat(fields(collector.list.getLast())).containsEntry("golmok.scope", "lifecycle").doesNotContainKey("trace.id");
    }
    @Test void p0ConformanceIncludesSchemaVersionInJsonAndOtlp() throws Exception {
        MDC.clear();
        MDC.put("password_secret_key", "RAW_PASSWORD_MUST_NOT_APPEAR");
        MDC.put("custom_unreviewed", "RAW_CUSTOM_MUST_NOT_APPEAR");
        MDC.put("invalid raw key", "RAW_INVALID_MUST_NOT_APPEAR");
        MDC.put("X-Unreviewed", "RAW_UNKNOWN_HEADER_MUST_NOT_APPEAR");
        MDC.put("unreviewed_trace", "ffffffffffffffffffffffffffffffff");
        MDC.put("unreviewed_span", "ffffffffffffffff");
        MDC.put("X-User-Id", "forged-user");
        try (Scope traced = span(); Scope user = identity()) {
            logger.atInfo().addKeyValue("golmok.executor.id", "worker-42").log("P0 schema contract");
        }
        Map<String, String> json = fields(collector.list.getFirst());
        assertThat(json).containsEntry("golmok.schema.version", "2").containsEntry("trace.id", TRACE)
                .containsEntry("span.id", SPAN).containsEntry("golmok.scope", "request")
                .containsEntry("golmok.executor.id", "worker-42").containsEntry("golmok.attributes.unmapped_count", "7")
                .containsKeys("@timestamp", "event.id", "service.name", "deployment.environment.name", "log.level");
        assertThat(json.entrySet().stream().filter(item -> item.getKey().startsWith("attributes.")).toList())
                .isEmpty();
        var record = exporter.getFinishedLogRecordItems().getFirst();
        assertThat(record.getAttributes().get(AttributeKey.longKey("golmok.schema.version"))).isEqualTo(2L);
        assertThat(record.getAttributes().get(AttributeKey.longKey("golmok.attributes.unmapped_count"))).isEqualTo(7L);
        assertThat(record.getAttributes().get(AttributeKey.stringKey("event.id"))).isEqualTo(json.get("event.id"));
        assertThat(new String(encoder.encode(collector.list.getFirst()), StandardCharsets.UTF_8)).contains("\"version\":2")
                .doesNotContain("password_secret_key", "custom_unreviewed", "invalid raw key", "X-Unreviewed", "RAW_PASSWORD", "RAW_CUSTOM", "RAW_INVALID", "RAW_UNKNOWN_HEADER", "forged-user");
        assertThat(record.getAttributes().toString()).doesNotContain("password_secret_key", "custom_unreviewed", "invalid raw key", "X-Unreviewed",
                "RAW_PASSWORD", "RAW_CUSTOM", "RAW_INVALID", "RAW_UNKNOWN_HEADER", "forged-user");
        MDC.clear();
        for (int index = 0; index < 35; index++) MDC.put("unreviewed_" + index, "RAW_VALUE");
        MDC.put("invalid key", "RAW_VALUE"); MDC.put("k".repeat(129), "RAW_VALUE");
        logger.info("bounded unknown metadata");
        Map<String, String> bounded = fields(collector.list.getLast());
        assertThat(bounded).containsEntry("golmok.attributes.unmapped_count", "37");
        assertThat(bounded.keySet().stream().filter(key -> key.startsWith("attributes.")).count()).isZero();
    }
    private AccessLogFilter filter() { return new AccessLogFilter(new AccessLogger(), new CommonLoggingProperties()); }
    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/members/private-id");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/members/{id}");
        return request;
    }
    private Map<String, String> fields(ILoggingEvent event) throws IOException {
        TelemetryFieldRegistry.load().validateSnapshot(CanonicalLogSnapshot.snapshot(event));
        Map<String, String> flat = new LinkedHashMap<>();
        try (JsonParser json = new JsonFactory().createParser(encoder.encode(event))) {
            assertThat(json.nextToken()).isEqualTo(JsonToken.START_OBJECT);
            readObject(json, "", flat);
        }
        return flat;
    }
    private void readObject(JsonParser json, String prefix, Map<String, String> flat) throws IOException {
        while (json.nextToken() != JsonToken.END_OBJECT) {
            String name = prefix + json.currentName(); JsonToken type = json.nextToken();
            if (type == JsonToken.START_OBJECT) readObject(json, name + ".", flat);
            else if (type == JsonToken.START_ARRAY) json.skipChildren();
            else assertThat(flat.put(name, json.getValueAsString())).as("unique canonical field %s", name).isNull();
        }
    }
}
