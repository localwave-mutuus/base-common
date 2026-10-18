package ai.mutuus.common.core;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OtelEnvelopeContractTest {
    private InMemorySpanExporter exporter;
    private SdkTracerProvider provider;

    @BeforeEach
    void setup() {
        GlobalOpenTelemetry.resetForTest();
        exporter = InMemorySpanExporter.create();
        provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
        OpenTelemetrySdk.builder().setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).buildAndRegisterGlobal();
    }

    @AfterEach
    void cleanup() { provider.close(); GlobalOpenTelemetry.resetForTest(); }

    @Test
    void captureAndOwnerValidatedExtractionRoundTripOnlyW3cAndVerifiedInitiator() {
        Span parent = span("publisher");
        TelemetryEnvelope envelope;
        try (Scope traced = parent.makeCurrent(); Scope user = identity("operator-7");
             Scope untrusted = Baggage.builder().put("jwt", "never-store").put("user.id", "forged").build().makeCurrent()) {
            envelope = TelemetryEnvelope.captureCurrentVerified("operation-1", "attempt-1");
        } finally { parent.end(); }
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.initiatorId()).isEqualTo("operator-7");
        assertThat(envelope.source()).isEqualTo("verified_initiator");
        assertThat(UUID.fromString(envelope.eventId())).isNotNull();
        assertThat(envelope.publishedAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(envelope.toString()).doesNotContain("never-store", "forged", "jwt");
        Context restored = envelope.extractFromOwnerValidatedStorage();
        assertThat(Span.fromContext(restored).getSpanContext().getTraceId()).isEqualTo(parent.getSpanContext().getTraceId());
        assertThat(Span.fromContext(restored).getSpanContext().getSpanId()).isEqualTo(parent.getSpanContext().getSpanId());
        assertThat(Baggage.fromContext(restored).isEmpty()).isTrue();
        try (Scope ignored = restored.makeCurrent()) {
            assertThat(VerifiedIdentityContext.current().fields()).containsEntry("user.id", "operator-7")
                    .containsEntry("golmok.identity.realm", "operator")
                    .containsEntry("golmok.identity.source", "verified_initiator");
        }
        assertThat(VerifiedIdentityContext.currentUserId()).isNull();
    }

    @Test
    void retryCreatesDistinctConsumerSpansWithSameStoredParentAndRestoresAmbientScope() {
        Span publisher = span("publisher");
        TelemetryEnvelope envelope;
        try (Scope traced = publisher.makeCurrent(); Scope user = identity("operator-7")) {
            envelope = TelemetryEnvelope.captureCurrentVerified("operation-1", "attempt-1");
        } finally { publisher.end(); }
        TelemetryEnvelope retry = envelope.withAttemptId("attempt-2");
        assertThat(retry.eventId()).isEqualTo(envelope.eventId());
        assertThat(retry.traceparent()).isEqualTo(envelope.traceparent());
        assertThat(retry.publishedAt()).isEqualTo(envelope.publishedAt());
        Span ambient = span("unrelated");
        try (Scope traced = ambient.makeCurrent(); Scope user = identity("operator-other")) {
            for (TelemetryEnvelope attempt : List.of(envelope, retry)) {
                try (var consumer = attempt.openConsumerFromOwnerValidatedStorage("queue.consume")) {
                    assertThat(Span.current().getSpanContext().getTraceId()).isEqualTo(publisher.getSpanContext().getTraceId());
                    assertThat(VerifiedIdentityContext.currentUserId()).isEqualTo("operator-7");
                    assertThat(TelemetryMetadata.current()).containsEntry("operation.id", "operation-1")
                            .containsEntry("golmok.attempt.id", attempt.attemptId())
                            .containsEntry("golmok.message.event.id", envelope.eventId()).doesNotContainKey("event.id");
                    assertThat(ai.mutuus.common.logging.CanonicalLogSnapshot.context())
                            .containsEntry("golmok.identity.source", "verified_initiator")
                            .containsEntry("golmok.scope", "job")
                            .containsEntry("golmok.message.event.id", envelope.eventId())
                            .containsEntry("golmok.attempt.id", attempt.attemptId()).containsKey("execution.id");
                    if (attempt == envelope) consumer.fail();
                }
                assertThat(Span.current().getSpanContext()).isEqualTo(ambient.getSpanContext());
                assertThat(VerifiedIdentityContext.currentUserId()).isEqualTo("operator-other");
                assertThat(TelemetryMetadata.current()).isEmpty();
            }
        } finally { ambient.end(); }
        var consumers = exporter.getFinishedSpanItems().stream().filter(item -> item.getKind() == SpanKind.CONSUMER).toList();
        assertThat(consumers).hasSize(2);
        assertThat(consumers.get(0).getSpanId()).isNotEqualTo(consumers.get(1).getSpanId());
        assertThat(consumers.get(0).getAttributes().get(AttributeKey.stringKey("execution.id")))
                .isNotEqualTo(consumers.get(1).getAttributes().get(AttributeKey.stringKey("execution.id")));
        for (var recorded : consumers) {
            assertThat(recorded.getParentSpanId()).isEqualTo(publisher.getSpanContext().getSpanId());
            assertThat(recorded.getAttributes().get(AttributeKey.stringKey("golmok.message.event.id"))).isEqualTo(envelope.eventId());
            assertThat(recorded.getAttributes().get(AttributeKey.stringKey("operation.id"))).isEqualTo("operation-1");
            assertThat(recorded.getAttributes().get(AttributeKey.stringKey("golmok.identity.source"))).isEqualTo("verified_initiator");
        }
        assertThat(consumers.get(0).getAttributes().get(AttributeKey.stringKey("golmok.attempt.id"))).isEqualTo("attempt-1");
        assertThat(consumers.get(1).getAttributes().get(AttributeKey.stringKey("golmok.attempt.id"))).isEqualTo("attempt-2");
        assertThat(consumers.get(0).getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
    }

    @Test
    void malformedVersionIdsBoundsAndIdentityAreRejectedWithoutFallbackTrace() {
        String parent = "00-1234567890abcdef1234567890abcdef-1234567890abcdef-01";
        String event = UUID.randomUUID().toString();
        assertThatThrownBy(() -> envelope(2, parent, null, null, null, null, event, "attempt-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envelope(1, "00-00000000000000000000000000000000-1234567890abcdef-01", null,
                null, null, null, event, "attempt-1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envelope(1, parent, "invalid", null, null, null, event, "attempt-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envelope(1, parent, null, "operator", null, "verified_initiator", event, "attempt-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envelope(1, parent, null, null, null, null, event, "x".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envelope(1, parent, null, null, null, null, "not-an-event-id", "attempt-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingCurrentSpanIsRejectedAndUnownedInitiatorIsNeverInvented() {
        try (Scope ignored = Context.root().makeCurrent()) {
            assertThatThrownBy(() -> TelemetryEnvelope.captureCurrentVerified(null, "attempt-1"))
                    .isInstanceOf(IllegalStateException.class);
        }
        Span publisher = span("workload");
        TelemetryEnvelope envelope;
        try (Scope ignored = publisher.makeCurrent()) {
            envelope = TelemetryEnvelope.captureCurrentVerified(null, "attempt-1");
        } finally { publisher.end(); }
        assertThat(envelope.initiatorId()).isNull();
        assertThat(envelope.realm()).isNull();
        try (Scope ignored = envelope.extractFromOwnerValidatedStorage().makeCurrent()) {
            assertThat(VerifiedIdentityContext.current().authState()).isEqualTo("workload");
            assertThat(VerifiedIdentityContext.currentUserId()).isNull();
        }
    }

    private Span span(String name) { return provider.get("envelope-contract").spanBuilder(name).setNoParent().startSpan(); }
    private Scope identity(String id) {
        return VerifiedIdentityContext.makeCurrent(new VerifiedIdentityContext.Identity(id, "operator", "authenticated"));
    }
    private TelemetryEnvelope envelope(int version, String parent, String state, String id, String realm,
            String source, String event, String attempt) {
        return new TelemetryEnvelope(version, parent, state, id, realm, source, event, Instant.now(), null, attempt);
    }
}