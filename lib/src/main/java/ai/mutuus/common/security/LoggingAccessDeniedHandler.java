package ai.mutuus.common.security;

import java.io.IOException;

import ai.mutuus.common.logging.AccessLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 인가 거부(403) 로깅 후 기본 동작에 위임한다.
 * <p>delegate 는 자원 서버의 {@code BearerTokenAccessDeniedHandler} 로 표준 403 응답을 보존한다.
 */
public class LoggingAccessDeniedHandler implements AccessDeniedHandler {

    private final AccessDeniedHandler delegate;
    private final AccessLogger accessLogger;

    public LoggingAccessDeniedHandler(AccessDeniedHandler delegate, AccessLogger accessLogger) {
        this.delegate = delegate;
        this.accessLogger = accessLogger;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        accessLogger.accessDenied(request.getRequestURI(), accessDeniedException.getMessage());
        delegate.handle(request, response, accessDeniedException);
    }
}
