package ai.mutuus.common.web;

import java.io.IOException;
import java.security.Principal;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Runs inside the security chain's authenticated request and outside MVC child spans. */
public final class TraceKeepFilter extends OncePerRequestFilter {
    private final TraceKeepUserSource users;
    private final TraceKeepLoginIdExtractor extractor;

    public TraceKeepFilter(TraceKeepUserSource users, TraceKeepLoginIdExtractor extractor) {
        this.users = users;
        this.extractor = extractor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // The servlet agent owns this SERVER span, including when it has a remote parent.
        Span server = Span.current();
        mark(request, server);
        try {
            chain.doFilter(request, response);
        } finally {
            // Login handlers may expose an ID only after safely reading their own body.
            mark(request, server);
        }
    }

    private void mark(HttpServletRequest request, Span server) {
        try {
            Principal principal = request.getUserPrincipal();
            String loginId = principal == null ? extractor.extract(request) : principal.getName();
            if (loginId != null && !loginId.isBlank() && users.loginIds().contains(loginId)) {
                server.setAttribute("golmok.trace.keep", true);
                server.setAttribute("enduser.id", loginId);
            }
        } catch (RuntimeException ignored) {
            // Optional retention metadata must not change authentication or request outcomes.
        }
    }
}
