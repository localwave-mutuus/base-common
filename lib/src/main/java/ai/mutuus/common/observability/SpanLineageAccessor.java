package ai.mutuus.common.observability;

import java.util.Optional;

/** SDK onStart data only. Headers, MDC and user KV are not lineage sources.
 * The agent-to-application class-loader bridge must be verified before installing an implementation.
 */
@FunctionalInterface
public interface SpanLineageAccessor {
    Optional<String> parentId(String traceId, String spanId);

    /** Explicit disconnected state; absence is not evidence of an actual root span. */
    static SpanLineageAccessor unavailable() { return (trace, span) -> Optional.empty(); }
}
