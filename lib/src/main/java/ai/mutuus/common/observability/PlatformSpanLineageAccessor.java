package ai.mutuus.common.observability;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
/** JDK-only bridge; the agent and application do not share library class identity. */
public final class PlatformSpanLineageAccessor implements SpanLineageAccessor {
    private static final ObjectName NAME;
    private static final AtomicLong unavailable = new AtomicLong();
    static { try { NAME = new ObjectName("ai.mutuus.common:type=SpanLineage"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    public static long unavailableCount() { return unavailable.get(); }
    @Override public Optional<String> parentId(String traceId, String spanId) {
        try {
            Object value = ManagementFactory.getPlatformMBeanServer().invoke(NAME, "parentId",
                new Object[]{traceId, spanId}, new String[]{"java.lang.String", "java.lang.String"});
            return value instanceof String parent && !parent.isEmpty() ? Optional.of(parent) : Optional.empty();
        } catch (Exception ignored) {
            unavailable.updateAndGet(n -> n == Long.MAX_VALUE ? n : n + 1); return Optional.empty();
        }
    }
}
