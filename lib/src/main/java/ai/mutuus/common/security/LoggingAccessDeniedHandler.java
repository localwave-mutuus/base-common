package ai.mutuus.common.security;

import java.io.IOException;

import ai.mutuus.common.logging.AccessLogger;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 인가 거부(403) 로깅 후 기본 동작에 위임한다.
 * <p>delegate 는 자원 서버의 {@code BearerTokenAccessDeniedHandler} 로 표준 403 응답을 보존한다.
 * <p>트래픽 로그({@link AccessLogger}의 {@code auth.denied})에 더해, <b>보안 감사 로그</b>
 * ({@link SecurityAuditLogger}의 {@code security.authz.denied}, 주체·clientIp 포함)를 함께 남긴다.
 */
public class LoggingAccessDeniedHandler implements AccessDeniedHandler {

    private final AccessDeniedHandler delegate;
    private final AccessLogger accessLogger;
    private final SecurityAuditLogger securityAuditLogger;

    public LoggingAccessDeniedHandler(AccessDeniedHandler delegate, AccessLogger accessLogger,
                                      SecurityAuditLogger securityAuditLogger) {
        this.delegate = delegate;
        this.accessLogger = accessLogger;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        accessLogger.accessDenied(request.getRequestURI(), accessDeniedException.getMessage());
        securityAuditLogger.authzDenied(request.getRequestURI(), request.getRemoteAddr(),
                currentPrincipal(), accessDeniedException.getMessage());
        delegate.handle(request, response, accessDeniedException);
    }

    /** 현재 인증 주체명(없으면 null) — 로그의 {@code principal} 필드용. */
    private static String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }
}
