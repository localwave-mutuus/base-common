package ai.mutuus.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2-A(HTTP 클라이언트 에러 디코딩) 검증 — 아웃바운드 read timeout 이 표준 예외 흐름을 거쳐
 * <b>504 GATEWAY_TIMEOUT</b> 으로 변환됨을 실서버(RANDOM_PORT)로 확인한다.
 * <p>{@code /demo/error/timeout} 은 짧은 read timeout(300ms)으로 느린 다운스트림(1.2s)을 호출한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpTimeoutIntegrationTest {

    @LocalServerPort
    int port;

    @Test
    void 아웃바운드_read_timeout은_504_GATEWAY_TIMEOUT으로_변환된다() {
        HttpStatusCode status = RestClient.create().get()
                .uri("http://localhost:" + port + "/demo/error/timeout")
                .exchange((request, response) -> response.getStatusCode()); // 오류 상태에도 예외 없이 상태만 확인

        assertThat(status.value()).isEqualTo(504);
    }
}
