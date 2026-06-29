package ai.mutuus.common.security;

import java.io.IOException;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.StringUtils;
import ai.mutuus.common.core.TraceContext;
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
 * 인증 완료 후 실제 인증 주체(JWT subject 등)를 추적/로깅 컨텍스트에 반영한다.
 * <p>보안 필터 체인의 {@code AuthorizationFilter} 뒤에 배치되어 인증이 확정된 시점에 동작한다.
 * 인입 헤더 {@code X-User-Id}(게이트웨이 전파값) 대신 <b>검증된 인증 주체</b>를 우선시하여
 * 이후 모든 로그(특히 {@code request.completed})와 {@code TraceContext.userId()}에 반영한다.
 * <p>정리는 최외곽 {@code TraceFilter} 의 {@code finally}(MDC/TraceContext clear)가 담당한다.
 */
public class AuthenticatedUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String userId = auth.getName();
            if (StringUtils.hasText(userId)) {
                TraceContext.put(HeaderNames.USER_ID, userId);
                MDC.put(HeaderNames.USER_ID, userId);
            }
        }
        filterChain.doFilter(request, response);
    }
}
