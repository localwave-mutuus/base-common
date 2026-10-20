package ai.mutuus.common.observability;

import io.opentelemetry.api.trace.SpanContext;
import java.util.Map;
import java.util.LinkedHashMap;

/** Immutable actual-span identity; a missing SDK connection never supplies a guessed parent. */
public record SpanLineageSnapshot(String traceId, String spanId, int traceFlags, String parentId) {
    public SpanLineageSnapshot {
        if (!valid(traceId, 32) || !valid(spanId, 16) || traceFlags < 0 || traceFlags > 255
                || (parentId != null && (!valid(parentId, 16) || parentId.equals(spanId))))
            throw new IllegalArgumentException("Invalid span lineage");
    }

    public static SpanLineageSnapshot from(SpanContext current, SpanLineageAccessor accessor) {
        if (!current.isValid()) throw new IllegalArgumentException("Valid current span required");
        String parent = accessor.parentId(current.getTraceId(), current.getSpanId()).orElse(null);
        return new SpanLineageSnapshot(current.getTraceId(), current.getSpanId(),
                Byte.toUnsignedInt(current.getTraceFlags().asByte()), parent);
    }

    public Map<String, Object> fields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("trace.id", traceId);
        fields.put("span.id", spanId);
        fields.put("trace.flags", traceFlags);
        if (parentId != null) fields.put("parent.id", parentId);
        return Map.copyOf(fields);
    }

    private static boolean valid(String value, int size) {
        return value != null && value.matches("[0-9a-f]{" + size + "}") && !value.matches("0+");
    }
}
