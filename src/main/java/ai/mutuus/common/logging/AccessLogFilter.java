package ai.mutuus.common.logging;

import java.io.IOException;

import ai.mutuus.common.core.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 액세스 로깅 필터. 요청 수신/응답 완료를 자동 기록한다.
 * <p>{@code TraceFilter}({@link Ordered#HIGHEST_PRECEDENCE}) <b>바로 뒤</b>에 위치하여
 * 추적 컨텍스트가 채워진 상태로 로깅하고, 보안 필터 체인을 <b>감싸므로</b> 응답 완료 로그가
 * 인증 실패(401/403)를 포함한 최종 상태코드를 본다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessLogFilter extends OncePerRequestFilter {

    private final AccessLogger accessLogger;
    private final CommonLoggingProperties props;

    public AccessLogFilter(AccessLogger accessLogger, CommonLoggingProperties props) {
        this.accessLogger = accessLogger;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        String query = props.isIncludeQueryString() ? request.getQueryString() : null;
        long startNanos = System.nanoTime();
        accessLogger.requestReceived(method, path, query, clientIp(request));
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            long threshold = props.getSlowRequestThresholdMillis();
            boolean slow = threshold > 0 && durationMs >= threshold;
            accessLogger.requestCompleted(method, path, response.getStatus(), durationMs, slow);
        }
    }

    private boolean isExcluded(String path) {
        for (String prefix : props.getExcludePathPrefixes()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** 프록시 뒤를 고려해 X-Forwarded-For 첫 홉 우선, 없으면 원격 주소. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
