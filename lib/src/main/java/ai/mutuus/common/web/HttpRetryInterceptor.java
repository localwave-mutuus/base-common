package ai.mutuus.common.web;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 아웃바운드 재시도 인터셉터 — <b>멱등 메서드(GET/HEAD/OPTIONS)</b> 요청이 {@link IOException}
 * (연결 실패·타임아웃 등)으로 실패하면 최대 {@code maxAttempts} 까지 재실행한다.
 * <p>비(非)멱등 메서드(POST 등)는 <b>재시도하지 않는다</b>(중복 처리 위험). 상태코드 기반 재시도도
 * 하지 않는다(응답 본문 소비/재사용 문제 회피). 백오프 없는 즉시 재시도다.
 */
public class HttpRetryInterceptor implements ClientHttpRequestInterceptor {

    private final int maxAttempts;

    public HttpRetryInterceptor(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (!isIdempotent(request.getMethod())) {
            return execution.execute(request, body);
        }
        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return execution.execute(request, body);
            } catch (IOException e) {
                last = e; // 다음 시도
            }
        }
        throw (last != null) ? last : new IOException("retry exhausted");
    }

    private boolean isIdempotent(HttpMethod method) {
        return HttpMethod.GET.equals(method)
                || HttpMethod.HEAD.equals(method)
                || HttpMethod.OPTIONS.equals(method);
    }
}
