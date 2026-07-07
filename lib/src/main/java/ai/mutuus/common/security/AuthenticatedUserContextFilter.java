package ai.mutuus.common.security;

import java.io.IOException;

import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.StringUtils;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 인증 완료 후 실제 인증 주체(JWT subject 등)를 추적/로깅 컨텍스트에 반영하고, 인입 {@code X-User-Id}(신뢰 불가)
 * 위장을 탐지한다.
 * <p>보안 필터 체인의 {@code AuthorizationFilter} 뒤에 배치되어 인증이 확정된 시점에 동작한다.
 * 인입 헤더 {@code X-User-Id}(게이트웨이 전파값) 대신 <b>검증된 인증 주체</b>를 우선시하여 이후 모든 로그와
 * {@code TraceContext.userId()}(→ 감사 created_by)에 반영한다.
 * <ul>
 *   <li>인증됨 + 주장값({@link TraceFilter#CLAIMED_USER_ATTR})이 주체와 <b>다름</b> → {@code security.identity.mismatch}(ERROR)</li>
 *   <li>미인증 + 주장값 존재 + 미신뢰 → {@code security.identity.header_ignored}(WARN, 위장값 폐기)</li>
 * </ul>
 * <p>정리는 최외곽 {@code TraceFilter} 의 {@code finally}(MDC/TraceContext clear)가 담당한다.
 */
public class AuthenticatedUserContextFilter extends OncePerRequestFilter {

    private final SecurityAuditLogger securityAuditLogger;
    private final boolean trustForwardedUser;

    public AuthenticatedUserContextFilter(SecurityAuditLogger securityAuditLogger, boolean trustForwardedUser) {
        this.securityAuditLogger = securityAuditLogger;
        this.trustForwardedUser = trustForwardedUser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
        String claimed = (String) request.getAttribute(HeaderNames.CLAIMED_USER_ATTR);

        if (authenticated && StringUtils.hasText(auth.getName())) {
            String principal = auth.getName();
            TraceContext.put(HeaderNames.USER_ID, principal); // 검증된 주체가 위장값보다 우선
            MDC.put(HeaderNames.USER_ID, principal);
            MDC.put(EcsFields.USER_ID, principal); // ECS alias(로깅 렌더 전용)
            if (StringUtils.hasText(claimed) && !claimed.equals(principal)) {
                securityAuditLogger.identityMismatch(request.getRequestURI(), request.getRemoteAddr(),
                        claimed, principal); // 신원 위·변조 시도
            }
        } else if (StringUtils.hasText(claimed) && !trustForwardedUser) {
            // 미인증인데 X-User-Id 를 주장 → 위장 시도. 신뢰하지 않으므로 폐기됨을 기록.
            securityAuditLogger.identityHeaderIgnored(request.getRequestURI(), request.getRemoteAddr(), claimed);
        }
        filterChain.doFilter(request, response);
    }
}
