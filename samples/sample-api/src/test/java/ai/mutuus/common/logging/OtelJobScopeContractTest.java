package ai.mutuus.common.logging;

import ai.mutuus.common.core.TelemetryMetadata;
import ai.mutuus.common.core.TelemetryScope;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.core.VerifiedIdentityContext;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OtelJobScopeContractTest {
    @Test void linkedRootDoesNotInheritParentOrUserAndRestoresCaller() {
        GlobalOpenTelemetry.resetForTest();
        var exporter = InMemorySpanExporter.create();
        try (var provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()) {
            OpenTelemetrySdk.builder().setTracerProvider(provider).buildAndRegisterGlobal();
            Span cause = GlobalOpenTelemetry.getTracer("contract").spanBuilder("cause").startSpan();
            try (Scope caller = cause.makeCurrent(); Scope identity = VerifiedIdentityContext.makeCurrent(
                    new VerifiedIdentityContext.Identity("member-7", "member", "authenticated"))) {
                try (var job = new JobPairLogger().beginRootLinked("linked-job", "operation-1", cause.getSpanContext())) {
                    assertThat(TraceContext.userId()).isNull();
                    assertThat(job.traceId()).isNotEqualTo(cause.getSpanContext().getTraceId());
                    assertThat(TelemetryScope.current()).isEqualTo("job");
                    assertThat(CanonicalLogSnapshot.context()).containsEntry("job.name", "linked-job")
                            .containsEntry("operation.id", "operation-1").containsEntry("execution.id", job.executionId());
                    job.success();
                }
                assertThat(TraceContext.userId()).isEqualTo("member-7");
                assertThat(TraceContext.spanId()).isEqualTo(cause.getSpanContext().getSpanId());
                assertThat(TelemetryScope.jobMetadata()).isEmpty();
            } finally { cause.end(); }
            var job = exporter.getFinishedSpanItems().stream().filter(s -> s.getName().equals("linked-job")).findFirst().orElseThrow();
            assertThat(job.getParentSpanContext().isValid()).isFalse();
            assertThat(job.getLinks()).hasSize(1);
            assertThat(job.getLinks().getFirst().getSpanContext()).isEqualTo(cause.getSpanContext());
        } finally { GlobalOpenTelemetry.resetForTest(); }
    }

    @Test void borrowedConsumerReusesVerifiedExecutionAndNeverEndsOwnerSpan() {
        GlobalOpenTelemetry.resetForTest();
        var exporter = InMemorySpanExporter.create();
        try (var provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()) {
            OpenTelemetrySdk.builder().setTracerProvider(provider).buildAndRegisterGlobal();
            Span consumer = GlobalOpenTelemetry.getTracer("contract").spanBuilder("consumer").startSpan();
            String execution = UUID.randomUUID().toString();
            consumer.setAttribute("execution.id", execution);
            try (Scope caller = TelemetryScope.with(Context.root().with(consumer), "request").makeCurrent();
                 Scope identity = VerifiedIdentityContext.makeCurrent(new VerifiedIdentityContext.Identity("member-7", "member", "authenticated"));
                 Scope metadata = TelemetryMetadata.makeCurrent(new TelemetryMetadata.Values("operation-1", "attempt-1", UUID.randomUUID().toString(), execution))) {
                try (var job = new JobPairLogger().beginInCurrentSpan("consume-job", "operation-1")) {
                    assertThat(job.executionId()).isEqualTo(execution);
                    assertThat(job.traceId()).isEqualTo(consumer.getSpanContext().getTraceId());
                    assertThat(TraceContext.userId()).isEqualTo("member-7");
                    assertThat(CanonicalLogSnapshot.context()).containsEntry("execution.id", execution)
                            .containsEntry("operation.id", "operation-1").containsEntry("job.name", "consume-job");
                    job.fail("EXPECTED_FAILURE");
                }
                assertThat(exporter.getFinishedSpanItems()).isEmpty();
                assertThat(consumer.isRecording()).isTrue();
                assertThat(TelemetryScope.current()).isEqualTo("request");
                assertThat(TelemetryScope.jobMetadata()).isEmpty();
                assertThat(TelemetryMetadata.current()).containsEntry("execution.id", execution);
            } finally { consumer.end(); }
            assertThat(exporter.getFinishedSpanItems()).hasSize(1);
            assertThat(exporter.getFinishedSpanItems().getFirst().getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        } finally { GlobalOpenTelemetry.resetForTest(); }
    }

    @Test void invalidParentOrMissingCurrentSpanIsRejected() {
        var logger = new JobPairLogger();
        assertThatThrownBy(() -> logger.beginRootLinked("job", "operation", SpanContext.getInvalid()))
                .isInstanceOf(IllegalArgumentException.class);
        try (Scope ignored = Context.root().makeCurrent()) {
            assertThatThrownBy(() -> logger.beginInCurrentSpan("job", "operation")).isInstanceOf(IllegalStateException.class);
        }
        var valid = SpanContext.create("1234567890abcdef1234567890abcdef", "1234567890abcdef",
                TraceFlags.getSampled(), TraceState.getDefault());
        try (Scope ignored = Context.root().with(Span.wrap(valid)).makeCurrent()) {
            assertThatThrownBy(() -> logger.beginInCurrentSpan("job", "operation"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("execution metadata");
        }
    }
}
