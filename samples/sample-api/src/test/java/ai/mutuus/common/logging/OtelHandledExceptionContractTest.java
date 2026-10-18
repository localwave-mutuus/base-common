package ai.mutuus.common.logging;

import java.nio.charset.StandardCharsets;
import java.util.*;
import ai.mutuus.common.core.VerifiedIdentityContext;
import ai.mutuus.common.exception.*;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.common.security.*;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.assertj.core.api.Assertions.*;

/** 실제 MVC 예외 처리→snapshot 인코더→완료 필터에서 처리된 예외 타입과 응답 비노출을 함께 검증한다. */
class OtelHandledExceptionContractTest {
    private record Binding(Logger logger, Level level, boolean additive) { }
    private final List<Binding> bindings = new ArrayList<>();
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();
    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private SnapshotAppender snapshot;
    private SdkTracerProvider provider;
    private MockMvc mvc;
    private AccessLogger access;
    private SecurityAuditLogger audit;

    @BeforeEach void setup() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        events.setContext(context); events.start();
        snapshot = new SnapshotAppender(); snapshot.setContext(context); snapshot.addAppender(events); snapshot.start();
        for (String name : List.of(AccessLogger.LOGGER_NAME, SecurityAuditLogger.LOGGER_NAME)) {
            Logger logger = context.getLogger(name);
            bindings.add(new Binding(logger, logger.getLevel(), logger.isAdditive()));
            logger.setLevel(Level.INFO); logger.setAdditive(false); logger.addAppender(snapshot);
        }
        provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
        access = new AccessLogger(); audit = new SecurityAuditLogger();
        var messages = new MessageResolver(new StaticMessageSource());
        mvc = MockMvcBuilders.standaloneSetup(new Controller())
                .setControllerAdvice(new GlobalExceptionHandler(messages, access, false), new SecurityExceptionAdvice(messages, audit))
                .addFilters(new AccessLogFilter(access, new CommonLoggingProperties())).build();
    }
    @AfterEach void cleanup() {
        bindings.forEach(binding -> { binding.logger().detachAppender(snapshot); binding.logger().setLevel(binding.level()); binding.logger().setAdditive(binding.additive()); });
        snapshot.stop(); provider.close();
    }

    @Test void handledBusinessValidationAnd403RetainActualTypeInErrorAndSingleCompletionWithoutResponseLeak() throws Exception {
        exercise("/business", 404, BusinessException.class, "error.business", false);
        exercise("/validation", 400, MethodArgumentNotValidException.class, "error.business", true);
        exercise("/denied", 403, AccessDeniedException.class, "security.authz.denied", false);
        exercise("/unexpected", 500, IllegalStateException.class, "error.server", false);
        assertThat(exporter.getFinishedSpanItems()).hasSize(4);
    }
    @Test void ordinary404WithoutExceptionHasNoInventedClassAndFilterSecurityHandlersRecordActualException() throws Exception {
        events.list.clear();
        Span plain = provider.get("handled-contract").spanBuilder("plain").startSpan();
        try (Scope scope = plain.makeCurrent()) {
            var request = new MockHttpServletRequest("GET", "/plain");
            var response = new MockHttpServletResponse();
            new AccessLogFilter(access, new CommonLoggingProperties()).doFilter(request, response,
                    (req, res) -> response.setStatus(404));
        } finally { plain.end(); }
        assertThat(events.list).hasSize(1);
        assertThat(CanonicalLogSnapshot.snapshot(events.list.getFirst())).doesNotContainKey("error.type");
        assertSecurity(false);
        assertSecurity(true);
    }

    private void exercise(String path, int status, Class<? extends Throwable> type, String action, boolean post) throws Exception {
        events.list.clear();
        Span span = provider.get("handled-contract").spanBuilder(path).setSpanKind(SpanKind.SERVER).startSpan();
        try (Scope scope = span.makeCurrent(); Scope identity = VerifiedIdentityContext.makeCurrent(
                new VerifiedIdentityContext.Identity("synthetic-member", "member", "authenticated"))) {
            var builder = post ? post(path).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}") : get(path);
            var response = mvc.perform(builder).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(status);
            assertThat(response.getContentAsString()).doesNotContain(type.getName(), "error.stack_trace", "private-unexpected-message");
            List<ILoggingEvent> completion = events.list.stream().filter(event -> "http.server.complete".equals(CanonicalLogSnapshot.snapshot(event).get("event.action"))).toList();
            List<ILoggingEvent> errors = events.list.stream().filter(event -> action.equals(CanonicalLogSnapshot.snapshot(event).get("event.action"))).toList();
            assertThat(completion).hasSize(1); assertThat(errors).hasSize(1);
            for (ILoggingEvent event : List.of(completion.getFirst(), errors.getFirst())) {
                Map<String, Object> fields = CanonicalLogSnapshot.snapshot(event);
                assertThat(fields).containsEntry("error.type", type.getName()).containsEntry("trace.id", span.getSpanContext().getTraceId())
                        .containsEntry("span.id", span.getSpanContext().getSpanId()).containsEntry("user.id", "synthetic-member");
                TelemetryFieldRegistry.load().validateSnapshot(fields);
                String json = new String(new CommonJsonEncoder().encode(event), StandardCharsets.UTF_8);
                assertThat(json).contains(type.getName());
                if (!"error.server".equals(action)) assertThat(fields).doesNotContainKey("error.stack_trace");
            }
            if ("/business".equals(path)) assertThat(CanonicalLogSnapshot.snapshot(errors.getFirst())).containsEntry("error.code", "MEM_NOT_FOUND");
        } finally { span.end(); }
    }
    private void assertSecurity(boolean denied) throws Exception {
        events.list.clear();
        var request = new MockHttpServletRequest("GET", "/security");
        var response = new MockHttpServletResponse();
        Span span = provider.get("handled-contract").spanBuilder("security").startSpan();
        Class<?> type = denied ? AccessDeniedException.class : BadCredentialsException.class;
        try (Scope scope = span.makeCurrent()) {
            new AccessLogFilter(access, new CommonLoggingProperties()).doFilter(request, response, (req, res) -> {
                if (denied) new LoggingAccessDeniedHandler((r, s, ex) -> s.setStatus(403), access, audit)
                        .handle((jakarta.servlet.http.HttpServletRequest) req, response, new AccessDeniedException("synthetic denied"));
                else new LoggingAuthenticationEntryPoint((r, s, ex) -> s.setStatus(401), access, audit)
                        .commence((jakarta.servlet.http.HttpServletRequest) req, response, new BadCredentialsException("synthetic rejected"));
            });
        } finally { span.end(); }
        assertThat(events.list.stream().filter(event -> "http.server.complete".equals(CanonicalLogSnapshot.snapshot(event).get("event.action")))).hasSize(1);
        assertThat(events.list).hasSize(3);
        events.list.forEach(event -> assertThat(CanonicalLogSnapshot.snapshot(event)).containsEntry("error.type", type.getName()).doesNotContainKey("error.stack_trace"));
    }
    enum PilotCode implements ErrorCode {
        MEM_NOT_FOUND;
        public String code() { return name(); }
        public HttpStatus status() { return HttpStatus.NOT_FOUND; }
        public String messageKey() { return "synthetic.member.notFound"; }
    }
    record Input(@NotBlank String name) { }
    @RestController static class Controller {
        @GetMapping("/business") public void business() { throw new BusinessException(PilotCode.MEM_NOT_FOUND); }
        @PostMapping("/validation") public void validation(@Valid @RequestBody Input input) { }
        @GetMapping("/denied") public void denied() { throw new AccessDeniedException("synthetic denied"); }
        @GetMapping("/unexpected") public void unexpected() { throw new IllegalStateException("private-unexpected-message"); }
    }
}