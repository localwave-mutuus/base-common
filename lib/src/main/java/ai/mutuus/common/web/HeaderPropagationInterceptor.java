package ai.mutuus.common.web;

import java.io.IOException;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.IdGenerator;
import ai.mutuus.common.core.TraceContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 아웃바운드 호출(RestClient/RestTemplate)에 e2e 헤더를 자동 부착한다.
 * <p>현재 {@link TraceContext}의 추적/화면/이벤트/단말/사용자 헤더를 하위 API로 전파하며,
 * span ID는 호출 구간마다 새로 발급한다. 이로써 API 간 추적 체인이 연결된다.
 */
public class HeaderPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        var headers = request.getHeaders();
        for (String name : HeaderNames.PROPAGATED) {
            if (HeaderNames.SPAN_ID.equals(name)) {
                continue;
            }
            TraceContext.get(name).ifPresent(v -> headers.set(name, v));
        }
        // 하위 호출 구간 식별을 위한 신규 span
        headers.set(HeaderNames.SPAN_ID, IdGenerator.newSpanId());
        return execution.execute(request, body);
    }
}
