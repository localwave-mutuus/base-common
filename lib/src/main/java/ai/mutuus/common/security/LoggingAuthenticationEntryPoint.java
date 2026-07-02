package ai.mutuus.common.security;

import java.io.IOException;

import ai.mutuus.common.logging.AccessLogger;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 인증 실패(401) 로깅 후 기본 동작에 위임한다.
 * <p>delegate 는 자원 서버의 {@code BearerTokenAuthenticationEntryPoint} 로,
 * {@code WWW-Authenticate} 헤더 등 표준 401 응답 의미를 보존한다.
 * <p>트래픽 로그({@link AccessLogger}의 {@code auth.failure})에 더해, <b>보안 감사 로그</b>
 * ({@link SecurityAuditLogger}의 {@code security.authn.failed}, clientIp 포함)를 함께 남긴다.
 */
public class LoggingAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint delegate;
    private final AccessLogger accessLogger;
    private final SecurityAuditLogger securityAuditLogger;

    public LoggingAuthenticationEntryPoint(AuthenticationEntryPoint delegate, AccessLogger accessLogger,
                                           SecurityAuditLogger securityAuditLogger) {
        this.delegate = delegate;
        this.accessLogger = accessLogger;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        accessLogger.authFailure(request.getRequestURI(), authException.getMessage());
        // 토큰 검증 실패(서명/만료/issuer/audience)는 security.jwt.rejected 로 사유를 분류, 그 외(토큰 없음 등)는 authn.failed
        if (authException instanceof OAuth2AuthenticationException oae) {
            OAuth2Error err = oae.getError();
            securityAuditLogger.jwtRejected(request.getRequestURI(), request.getRemoteAddr(),
                    err.getErrorCode(), err.getDescription());
        } else {
            securityAuditLogger.authnFailed(request.getRequestURI(), request.getRemoteAddr(),
                    authException.getMessage());
        }
        delegate.commence(request, response, authException);
    }
}
