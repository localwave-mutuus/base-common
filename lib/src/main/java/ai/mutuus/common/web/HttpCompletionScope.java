package ai.mutuus.common.web;

import ai.mutuus.common.logging.CanonicalLogSnapshot;
import ai.mutuus.common.observability.TelemetryContextSnapshot;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One request/attempt, one immutable completion. No span creation or header injection. */
public final class HttpCompletionScope {
    public record Completion(String requestId, Integer status, String outcome, String errorCode,
                             long durationNanos, Map<String,Object> snapshot) { }
    private final String id = UUID.randomUUID().toString();
    private final long started = System.nanoTime();
    private static final java.util.concurrent.atomic.AtomicLong writeFailures = new java.util.concurrent.atomic.AtomicLong();
    public static long writeFailureCount() { return writeFailures.get(); }
    private final AtomicBoolean finished = new AtomicBoolean();
    private final TelemetryContextSnapshot context = TelemetryContextSnapshot.capture();
    private final Map<String,Object> snapshot;
    private final ai.mutuus.common.observability.AgentClientObservation client;
    public void detachClientDispatch() { if (client != null) client.detach(); }
    public HttpCompletionScope() { this(true); }
    HttpCompletionScope(boolean includeLineage) {
        client = includeLineage ? null : new ai.mutuus.common.observability.AgentClientObservation();
        var fields = new LinkedHashMap<>(CanonicalLogSnapshot.context());
        if (!includeLineage) { fields.remove("trace.id"); fields.remove("span.id"); fields.remove("parent.id"); fields.remove("trace.flags"); }
        fields.put("golmok.request.id", id); snapshot = Map.copyOf(fields);
    }
    public String requestId() { return id; }
    public Map<String,Object> snapshot() { return snapshot; }
    public boolean finish(Integer status, String error, Consumer<Completion> writer) {
        return finish(status, error, Map.of(), writer);
    }
    public boolean finish(Integer status, String error, Map<String,Object> trustedMetadata, Consumer<Completion> writer) {
        if (!finished.compareAndSet(false, true)) return false;
        var fields = new LinkedHashMap<>(snapshot);
        if (client != null) {
            fields.putAll(client.finish());
            if (client.completedByTransport() && ObservedJdkHttpClient.transportLoggingEnabled()) return true;
        }
        trustedMetadata.forEach((key, value) -> {
            if (!Set.of("trace.id", "span.id", "parent.id", "trace.flags", "golmok.request.id").contains(key))
                fields.put(key, value);
        });
        String code = error != null && !error.isEmpty() ? error : status != null && status >= 400 ? "HTTP_" + status : null;
        String outcome = code != null || status == null ? "failure" : "success";
        try (var ignored = context.makeCurrent()) {
            try { writer.accept(new Completion(id, status, outcome, code, Math.max(0, System.nanoTime()-started), Map.copyOf(fields))); }
            catch (RuntimeException ignoredWriteFailure) { writeFailures.updateAndGet(n -> n == Long.MAX_VALUE ? n : n + 1); }
        }
        return true;
    }
    public static boolean confirmedDisconnect(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable e = error; e != null && seen.add(e); e = e.getCause()) {
            String type = e.getClass().getName();
            if (type.equals("org.apache.catalina.connector.ClientAbortException")
                || type.equals("org.eclipse.jetty.io.EofException")) return true;
        }
        return false;
    }
}
