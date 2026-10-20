package ai.mutuus.common.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Captures OTel Context without creating a span, installing an SDK or copying MDC. */
public final class TelemetryContextSnapshot {
    private static final SpanLineageAccessor LIVE = new PlatformSpanLineageAccessor();
    private static final ContextKey<SpanLineageSnapshot> CAPTURED = ContextKey.named("common.captured-lineage");
    private final Context context;
    private final SpanLineageSnapshot lineage;

    private TelemetryContextSnapshot(Context context, SpanLineageSnapshot lineage) {
        this.context = context;
        this.lineage = lineage;
    }

    /** Missing agent bridge omits parent.id and increments a bounded diagnostic counter. */
    public static TelemetryContextSnapshot capture() {
        Context context = Context.current();
        var active = Span.fromContext(context).getSpanContext();
        var captured = context.get(CAPTURED);
        if (captured != null && active.isValid() && captured.traceId().equals(active.getTraceId())
                && captured.spanId().equals(active.getSpanId())) return new TelemetryContextSnapshot(context, captured);
        return capture(LIVE);
    }

    public static TelemetryContextSnapshot capture(SpanLineageAccessor accessor) {
        Objects.requireNonNull(accessor);
        Context context = Context.current();
        var current = Span.fromContext(context).getSpanContext();
        return new TelemetryContextSnapshot(context,
                current.isValid() ? SpanLineageSnapshot.from(current, accessor) : null);
    }

    public Optional<SpanLineageSnapshot> lineage() { return Optional.ofNullable(lineage); }
    public Map<String, Object> fields() { return lineage == null ? Map.of() : lineage.fields(); }
    public Scope makeCurrent() { return (lineage == null ? context : context.with(CAPTURED, lineage)).makeCurrent(); }

    /** Snapshot data stays immutable even after the SDK removes the corresponding live entry. */
    public Runnable wrap(Runnable task) {
        Objects.requireNonNull(task);
        return () -> { try (Scope ignored = makeCurrent()) { task.run(); } };
    }
}
