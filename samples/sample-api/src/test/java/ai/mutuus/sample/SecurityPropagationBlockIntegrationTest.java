package ai.mutuus.sample;

import java.util.Map;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 HTTP 경계에서 미신뢰 대상의 커스텀 추적/식별 헤더 제거를 검증한다.
 * <p>CLIENT span/W3C 주입의 owner는 Java agent이며, 이 시험은 agent 활성화를 가정하지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "mutuus.common.propagation.allowed-hosts=internal.example.com")
class SecurityPropagationBlockIntegrationTest {
    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder restClientBuilder;

    @AfterEach
    void cleanup() { TraceContext.clear(); }

    @Test
    @SuppressWarnings("unchecked")
    void 미신뢰_호스트에는_직접_추가한_커스텀_추적과_식별헤더도_전달되지_않는다() {
        TraceContext.put(HeaderNames.TRACE_ID, "forged-trace");
        TraceContext.put(HeaderNames.SPAN_ID, "forged-span");
        TraceContext.put(HeaderNames.USER_ID, "forged-user");
        TraceContext.put(HeaderNames.APP_CODE, "synthetic-app");
        TraceContext.put(HeaderNames.INSTANCE_ID, "synthetic-instance");

        RestClient client = restClientBuilder.baseUrl("http://localhost:" + port).build();
        Map<String, Object> envelope = client.get().uri("/api/public/echo-headers")
                .headers(headers -> {
                    headers.set(HeaderNames.TRACE_ID, "forged-trace");
                    headers.set(HeaderNames.SPAN_ID, "forged-span");
                    headers.set(HeaderNames.USER_ID, "forged-user");
                    headers.set(HeaderNames.APP_CODE, "synthetic-app");
                    headers.set(HeaderNames.INSTANCE_ID, "synthetic-instance");
                }).retrieve().body(Map.class);
        assertThat(envelope).isNotNull();
        Map<String, Object> data = (Map<String, Object>) envelope.get("data");
        assertThat(data).containsEntry("traceId", "null")
                .containsEntry("spanId", "null")
                .containsEntry("userId", "null")
                .containsEntry("appCode", "null")
                .containsEntry("instanceCode", "null");
    }
}