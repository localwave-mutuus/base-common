package ai.mutuus.common.async;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.TraceHeaders;
import ai.mutuus.common.core.VerifiedIdentityContext;
import ai.mutuus.common.core.TelemetryScope;
import ai.mutuus.common.logging.JobPairLogger;
import ai.mutuus.common.web.TraceFilter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.StatusCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;

class OtelContextContractTest {
    private static final String TRACE = "1234567890abcdef1234567890abcdef";
    private static Span span(String id) {
        return Span.wrap(SpanContext.create(TRACE, id, TraceFlags.getSampled(), TraceState.getDefault()));
    }
    @Test void realProviderExportsIndependentJobAndBusinessChildOnExceptionalExit() throws Exception {
        GlobalOpenTelemetry.resetForTest();
        var exporter = InMemorySpanExporter.create();
        try (var provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()) {
            OpenTelemetrySdk.builder().setTracerProvider(provider).setPropagators(
                    io.opentelemetry.context.propagation.ContextPropagators.create(
                            io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance()))
                    .buildAndRegisterGlobal();
            Span request = GlobalOpenTelemetry.getTracer("contract").spanBuilder("request").startSpan();
            try (Scope ignored = request.makeCurrent()) {
                String remote = "00-" + request.getSpanContext().getTraceId() + "-1234567890abcdef-01";
                var inbound = TraceHeaders.inbound(key -> TraceHeaders.TRACEPARENT.equals(key) ? remote : "forged", true);
                assertThat(inbound.parentId()).isEqualTo("1234567890abcdef");
                assertThat(inbound.spanId()).isEqualTo(request.getSpanContext().getSpanId());
                assertThat(TraceHeaders.outbound(true)).containsOnlyKeys("traceparent");
                assertThat(TraceHeaders.outbound(true).get("traceparent")).contains(request.getSpanContext().getSpanId());
                assertThatThrownBy(() -> TraceContextPropagation.wrapBusiness("business", (Callable<Void>) () -> {
                    throw new IllegalStateException("expected");
                }).call()).isInstanceOf(IllegalStateException.class);
                try (var job = new JobPairLogger().begin("cron", "operation-1")) {
                    assertThat(TelemetryScope.current()).isEqualTo("job");
                    assertThat(job.traceId()).isNotEqualTo(request.getSpanContext().getTraceId());
                    assertThat(TraceContext.spanId()).isNotEqualTo(request.getSpanContext().getSpanId());
                    // 미완료 close도 오류 span을 종료해야 한다.
                }
                assertThat(TraceContext.spanId()).isEqualTo(request.getSpanContext().getSpanId());
                assertThat(TelemetryScope.current()).isEqualTo("request");
            } finally { request.end(); }
            var spans = exporter.getFinishedSpanItems();
            assertThat(spans).hasSize(3);
            var job = spans.stream().filter(s -> s.getName().equals("cron")).findFirst().orElseThrow();
            assertThat(job.getParentSpanContext().isValid()).isFalse();
            assertThat(job.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
            var child = spans.stream().filter(s -> s.getName().equals("business")).findFirst().orElseThrow();
            assertThat(child.getParentSpanId()).isEqualTo(request.getSpanContext().getSpanId());
            assertThat(child.getTraceId()).isEqualTo(request.getSpanContext().getTraceId());
            assertThat(child.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        } finally { GlobalOpenTelemetry.resetForTest(); }
    }
    @Test void currentSpanIsOnlyTraceSourceAndForgedHeadersAreIgnored() throws Exception {
        try (Scope ignored = Context.root().with(span("1234567890abcdef")).makeCurrent()) {
            TraceContext.put(HeaderNames.TRACE_ID, "forged");
            TraceContext.put(HeaderNames.USER_ID, "forged");
            assertThat(TraceContext.traceId()).isEqualTo(TRACE);
            assertThat(TraceContext.userId()).isNull();
            var request = new MockHttpServletRequest();
            request.addHeader(HeaderNames.TRACE_ID, "forged");
            request.addHeader(HeaderNames.USER_ID, "forged");
            var response = new MockHttpServletResponse();
            new TraceFilter("APP1", "ABC123", true).doFilter(request, response, (req, res) -> {
                assertThat(TraceContext.traceId()).isEqualTo(TRACE);
                assertThat(TraceContext.userId()).isNull();
            });
            assertThat(response.getHeader(HeaderNames.TRACE_ID)).isEqualTo(TRACE);
        } finally { TraceContext.clear(); }
        try (Scope ignored = Context.root().makeCurrent()) {
            assertThat(TraceContext.traceId()).isNull();
            assertThat(TraceHeaders.inbound(key -> "forged", true).traceId()).isNull();
        }
    }
    @Test void capturedOtelAndVerifiedIdentityRestoreOnReusedWorkerEvenOnFailure() throws Exception {
        Callable<String> captured;
        try (Scope ignored = Context.root().with(span("1234567890abcdef")).makeCurrent();
             Scope identity = VerifiedIdentityContext.makeCurrent(
                     new VerifiedIdentityContext.Identity("member-7", "member", "authenticated"))) {
            captured = TraceContextPropagation.wrap(() -> {
                assertThat(TraceContext.traceId()).isEqualTo(TRACE);
                assertThat(TraceContext.userId()).isEqualTo("member-7");
                throw new IllegalStateException("expected");
            });
        }
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try (Scope ignored = Context.root().with(span("abcdef1234567890")).makeCurrent()) {
                    assertThatThrownBy(captured::call).isInstanceOf(IllegalStateException.class);
                    assertThat(TraceContext.spanId()).isEqualTo("abcdef1234567890");
                    assertThat(TraceContext.userId()).isNull();
                }
            }).get();
            assertThat(executor.submit(() -> TraceContext.userId()).get()).isNull();
        }
    }
    @Test void independentJobDetachesRequestIdentityAndRestoresItOnClose() {
        try (Scope ignored = Context.root().with(span("1234567890abcdef")).makeCurrent();
             Scope identity = VerifiedIdentityContext.makeCurrent(
                     new VerifiedIdentityContext.Identity("member-7", "member", "authenticated"))) {
            try (var job = new JobPairLogger().begin("contract-job", "operation-1")) {
                assertThat(TelemetryScope.current()).isEqualTo("job");
                assertThat(TraceContext.userId()).isNull();
                job.cancel();
            }
            assertThat(TraceContext.traceId()).isEqualTo(TRACE);
            assertThat(TraceContext.userId()).isEqualTo("member-7");
        }
    }
}
