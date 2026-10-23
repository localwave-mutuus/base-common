package ai.mutuus.common.web;

import java.util.Set;

/** Supplies the current set of valid login IDs selected for trace retention. */
@FunctionalInterface
public interface TraceKeepUserSource {
    Set<String> loginIds();
}
