package ai.mutuus.common.observability;

import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import java.util.Map;
import io.opentelemetry.api.trace.Span;

/** Bounded JDK-only agent bridge. Does not create spans or write propagation headers. */
public final class AgentClientObservation {
    private final String token;
    private boolean transportCompleted;
    public boolean completedByTransport() { return transportCompleted; }
    public AgentClientObservation() {
        var current = Span.current().getSpanContext();
        Object result = invoke("beginClient", current.isValid() ? current.getTraceId() : "", current.isValid() ? current.getSpanId() : "");
        token = result instanceof String text ? text : "";
    }
    public void detach() { if (!token.isEmpty()) invoke("detachClient", token); }
    public Map<String,Object> finish() {
        Object result = token.isEmpty() ? null : invoke("finishClient", token);
        if (!(result instanceof Map<?,?> fields)) return Map.of();
        transportCompleted = Boolean.TRUE.equals(fields.get("_transport.completed"));
        try {
            var snapshot = new SpanLineageSnapshot((String) fields.get("trace.id"), (String) fields.get("span.id"),
                (Integer) fields.get("trace.flags"), (String) fields.get("parent.id"));
            return snapshot.fields();
        } catch (RuntimeException ignored) { return Map.of(); }
    }
    private static Object invoke(String action, String... values) {
        try {
            String[] signature = new String[values.length]; java.util.Arrays.fill(signature, "java.lang.String");
            return ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("ai.mutuus.common:type=SpanLineage"), action, values, signature);
        } catch (Exception ignored) { return null; }
    }
}
