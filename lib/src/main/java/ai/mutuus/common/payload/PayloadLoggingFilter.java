package ai.mutuus.common.payload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * 컨트롤러 입출력 본문 로깅 필터.
 * <p>모든 인입 요청을 컨트롤러 바깥에서 일괄 감싼다:
 * <ol>
 *   <li><b>진입</b>: 요청 본문을 캐싱({@link CachedBodyHttpServletRequest})해 컨트롤러가 다시 읽을 수
 *       있게 한 뒤, {@link RequestPayloadLogger}로 입력 파라미터를 출력.</li>
 *   <li><b>종료(finally)</b>: 응답 본문을 캐싱({@link ContentCachingResponseWrapper})해
 *       {@link ResponsePayloadLogger}로 리턴값을 출력하고, 캐싱 본문을 실제 응답에 복원.</li>
 * </ol>
 * 순서는 {@code HIGHEST_PRECEDENCE + 20}(AccessLogFilter 바로 안쪽, 컨트롤러에 가장 가깝다).
 * 토글/정책은 {@link PayloadLoggingProperties}. 진입/종료 출력 로직은 각각 별도 빈으로 분리돼 있다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PayloadLoggingFilter extends OncePerRequestFilter {

    private final RequestPayloadLogger requestLogger;
    private final ResponsePayloadLogger responseLogger;
    private final PayloadLoggingProperties props;

    public PayloadLoggingFilter(RequestPayloadLogger requestLogger, ResponsePayloadLogger responseLogger,
                                PayloadLoggingProperties props) {
        this.requestLogger = requestLogger;
        this.responseLogger = responseLogger;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 제외 경로(/actuator 등)는 래핑 비용 없이 그대로 통과시킨다.
        if (isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (props.getMode() == PayloadLoggingProperties.Mode.ERROR_ONLY) {
            logErrorsOnly(request, response, filterChain);
            return;
        }

        // 요청 본문은 스트림이라 한 번 읽으면 소진된다 → 캐싱 래퍼로 감싸 "진입 로깅"과 "컨트롤러"가 모두 읽게 한다.
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        // 응답 본문도 실제로 쓰이면서 버퍼에 캐싱되도록 감싼다(뒤에서 로깅 후 원본으로 복원).
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        // ① 진입: 입력 파라미터(요청 본문) 출력 — 컨트롤러 실행 전에 남긴다.
        requestLogger.onRequest(cachedRequest,
                bodyToLog(cachedRequest.getContentType(), cachedRequest.getCachedBody()));
        try {
            // ② 체인 실행 → 보안/컨트롤러까지 진행. 컨트롤러는 캐싱된 본문을 다시 읽는다.
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            // ③ 종료: 리턴값(응답 본문) 출력 — 예외로 빠져나가도 반드시 남기려 finally 에 둔다.
            try {
                responseLogger.onResponse(cachedRequest, cachedResponse.getStatus(),
                        bodyToLog(cachedResponse.getContentType(), cachedResponse.getContentAsByteArray()));
            } finally {
                // 로거가 실패해도 실제 응답 본문은 복원한다.
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private void logErrorsOnly(HttpServletRequest request, HttpServletResponse response,
                               FilterChain chain) throws ServletException, IOException {
        // 성공 요청도 결과가 나올 때까지 최대 4KiB만 보관한다. 원본 스트림은 그대로 소비한다.
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, props.getMaxBodyBytes());
        BoundedPayloadResponseWrapper cachedResponse = new BoundedPayloadResponseWrapper(response, props.getMaxBodyBytes());
        boolean failed = false;
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } catch (IOException | ServletException | RuntimeException | Error ex) {
            failed = true;
            throw ex;
        } finally {
            cachedResponse.flushWriter();
            if (failed || cachedResponse.getStatus() >= 400) {
                requestLogger.onRequest(cachedRequest,
                        bodyToLog(cachedRequest.getContentType(), cachedRequest.getContentAsByteArray()));
                responseLogger.onResponse(cachedRequest, failed ? 500 : cachedResponse.getStatus(),
                        bodyToLog(cachedResponse.getContentType(), cachedResponse.capturedBody()));
            }
        }
    }

    /** Content-Type/크기 정책을 적용해 로깅 문자열을 만든다. 본문이 없거나 대상 타입이 아니면 {@code null}. */
    private String bodyToLog(String contentType, byte[] body) {
        // 본문이 없으면 로깅 필드 자체를 생략(null)
        if (body == null || body.length == 0) {
            return null;
        }
        // 바이너리(이미지/파일 등)는 텍스트로 남기면 로그가 깨지고 무의미 → 크기만 표기
        if (!isLoggableContentType(contentType)) {
            return null;
        }
        int max = props.getMaxBodyBytes();
        // 잘린 다중 바이트 문자의 대체문자까지 포함해 UTF-8 출력 상한을 지킨다.
        String result = new String(body, 0, Math.min(body.length, max), StandardCharsets.UTF_8);
        while (result.getBytes(StandardCharsets.UTF_8).length > max) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean isLoggableContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        for (String prefix : props.getIncludeContentTypes()) {
            if (ct.startsWith(prefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(String path) {
        for (String prefix : props.getExcludePathPrefixes()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
