package ai.mutuus.common.observability;

import ai.mutuus.common.core.TelemetryTaskDecorator;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class TelemetryContextSnapshotTest {
    private static final String TRACE = "1234567890abcdef1234567890abcdef";
    private static final String SPAN = "1234567890abcdef";
    private static final String PARENT = "abcdef1234567890";
    private static final Span ACTIVE = Span.wrap(SpanContext.create(TRACE, SPAN, TraceFlags.getSampled(), TraceState.getDefault()));

    @Test void disconnectedStateHasNoParentAndInvalidContextHasNoIds() {
        try (Scope ignored = Context.root().makeCurrent()) {
            assertThat(TelemetryContextSnapshot.capture().fields()).isEmpty();
        }
        try (Scope ignored = ACTIVE.makeCurrent()) {
            assertThat(TelemetryContextSnapshot.capture().fields()).containsEntry("trace.id", TRACE)
                    .containsEntry("span.id", SPAN).doesNotContainKey("parent.id");
        }
    }

    @Test void actualAccessorSnapshotSurvivesLiveEntryRemoval() {
        AtomicReference<String> live = new AtomicReference<>(PARENT);
        TelemetryContextSnapshot snapshot;
        try (Scope ignored = ACTIVE.makeCurrent()) {
            snapshot = TelemetryContextSnapshot.capture((trace, span) -> {
                assertThat(trace).isEqualTo(TRACE); assertThat(span).isEqualTo(SPAN);
                return Optional.ofNullable(live.get());
            });
        }
        live.set(null);
        assertThat(snapshot.fields()).containsEntry("parent.id", PARENT);
        try (Scope ignored = snapshot.makeCurrent()) {
            assertThat(ai.mutuus.common.logging.CanonicalLogSnapshot.context()).containsEntry("parent.id", PARENT);
            try (Scope child = Span.wrap(SpanContext.create(TRACE, "bbbbbbbbbbbbbbbb", TraceFlags.getSampled(),
                    TraceState.getDefault())).makeCurrent()) {
                assertThat(TelemetryContextSnapshot.capture().fields()).doesNotContainKey("parent.id");
            }
        }
        assertThatThrownBy(() -> snapshot.fields().put("parent.id", SPAN)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void decoratorRestoresCapturedContextAndReleasesAfterThrowWithoutCreatingSpan() throws Exception {
        var key = ContextKey.<String>named("snapshot-fixture");
        Runnable decorated;
        try (Scope ignored = Context.root().with(ACTIVE).with(key, "captured").makeCurrent()) {
            decorated = new TelemetryTaskDecorator().decorate(() -> {
                assertThat(Span.current().getSpanContext()).isEqualTo(ACTIVE.getSpanContext());
                assertThat(Context.current().get(key)).isEqualTo("captured");
                throw new IllegalStateException("fixture");
            });
        }
        Thread worker = new Thread(() -> {
            try (Scope ignored = Context.root().with(key, "worker").makeCurrent()) {
                assertThatThrownBy(decorated::run).isInstanceOf(IllegalStateException.class);
                assertThat(Context.current().get(key)).isEqualTo("worker");
                assertThat(Span.current().getSpanContext().isValid()).isFalse();
            }
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        worker.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
        worker.start(); worker.join();
        assertThat(failure.get()).isNull();
    }

    @Test void nestedDecoratorPreservesCapturedActualParent() {
        TelemetryContextSnapshot snapshot;
        try (Scope ignored = ACTIVE.makeCurrent()) {
            snapshot = TelemetryContextSnapshot.capture((trace, span) -> Optional.of(PARENT));
        }
        Runnable decorated;
        try (Scope ignored = snapshot.makeCurrent()) {
            decorated = new TelemetryTaskDecorator().decorate(() ->
                    assertThat(TelemetryContextSnapshot.capture().fields()).containsEntry("parent.id", PARENT));
        }
        try (Scope ignored = Context.root().makeCurrent()) {
            decorated.run();
            assertThat(Span.current().getSpanContext().isValid()).isFalse();
        }
    }

    @Test void rejectsMalformedZeroAndSelfParentIds() {
        assertThatThrownBy(() -> new SpanLineageSnapshot(TRACE, SPAN, 1, SPAN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpanLineageSnapshot(TRACE, SPAN, 1, "0000000000000000")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpanLineageSnapshot(TRACE, SPAN, 256, PARENT)).isInstanceOf(IllegalArgumentException.class);
    }
}
