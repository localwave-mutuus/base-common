package ai.mutuus.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Optional login-request fallback when there is no authenticated principal.
 * Return null if unavailable. Never consume the request body: use a service-owned
 * replayable wrapper or an attribute populated by the login handler instead.
 * Called before and after the downstream chain; implementations must be idempotent.
 */
@FunctionalInterface
public interface TraceKeepLoginIdExtractor {
    String extract(HttpServletRequest request);
}
