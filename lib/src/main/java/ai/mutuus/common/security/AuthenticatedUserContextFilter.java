package ai.mutuus.common.security;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.VerifiedIdentityContext;
import ai.mutuus.common.core.VerifiedIdentityContext.Identity;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** 인증 이후 ExceptionTranslationFilter 앞에 두어 인증된 403도 검증 사용자로 기록한다.
 * <p>실제 인증 체인에 명시 등록한다. Servlet 필터 이중 등록을 피한다.
 */
public class AuthenticatedUserContextFilter extends OncePerRequestFilter {
    private final SecurityAuditLogger securityAuditLogger;
    private final AuthenticationIdentityAdapter identityAdapter;

    /** 호환용 생성자. 전달된 사용자 헤더 신뢰 설정은 폐기하고 검증 JWT만 사용한다. */
    public AuthenticatedUserContextFilter(SecurityAuditLogger audit, boolean trustForwardedUser) {
        this(audit, new JwtSubjectIdentityAdapter());
    }

    public AuthenticatedUserContextFilter(SecurityAuditLogger audit, AuthenticationIdentityAdapter adapter) {
        this.securityAuditLogger = Objects.requireNonNull(audit);
        this.identityAdapter = Objects.requireNonNull(adapter);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Identity identity = resolveIdentity();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        request.setAttribute(VerifiedIdentityContext.REQUEST_ATTRIBUTE, identity);
        try (Scope ignored = VerifiedIdentityContext.makeCurrent(identity)) {
            applyMdc(identity);
            applySpan(identity);
            String claimed = (String) request.getAttribute(HeaderNames.CLAIMED_USER_ATTR);
            if (claimed != null && !claimed.isBlank()) {
                if (identity.userId() != null && !claimed.equals(identity.userId())) {
                    securityAuditLogger.identityMismatch(request.getRequestURI(), request.getRemoteAddr(),
                            claimed, identity.userId());
                } else if (identity.userId() == null) {
                    securityAuditLogger.identityHeaderIgnored(request.getRequestURI(), request.getRemoteAddr(), claimed);
                }
            }
            try {
                chain.doFilter(request, response);
            } finally {
                // 하위 로그인 성공이나 SecurityContext 변경도 완료 스냅샷에 반영한다.
                Identity completed = resolveIdentity();
                if ("anonymous".equals(completed.authState()) && response.getStatus() == 401) {
                    completed = new Identity(null, null, "failed");
                }
                request.setAttribute(VerifiedIdentityContext.REQUEST_ATTRIBUTE, completed);
                applySpan(completed);
            }
        } finally {
            if (previousMdc == null) MDC.clear(); else MDC.setContextMap(previousMdc);
        }
    }

    private Identity resolveIdentity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return new Identity(null, null, "anonymous");
        }
        // 미지원 principal은 인증 상태만 기록하고 getName을 사용자 ID로 추측하지 않는다.
        return identityAdapter.resolve(auth).orElseGet(() -> new Identity(null, null, "authenticated"));
    }

    private static void applyMdc(Identity identity) {
        MDC.remove(HeaderNames.USER_ID);
        MDC.remove("user.id");
        MDC.remove("golmok.identity.realm");
        identity.fields().forEach(MDC::put);
    }

    private static void applySpan(Identity identity) {
        Span span = Span.current();
        if (span.getSpanContext().isValid()) identity.fields().forEach(span::setAttribute);
    }
}
