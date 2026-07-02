package ai.mutuus.sample;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>보안 응답 헤더(⑦) · CSRF 조건부(⑥)</b>(Phase 3) 통합 테스트.
 * <ul>
 *   <li>{@code headers.enabled=true} → 응답에 Referrer-Policy·Permissions-Policy</li>
 *   <li>{@code csrf-enabled=true} → CSRF 토큰 없는 상태변경(POST) 은 403</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "mutuus.common.security.headers.enabled=true",
        "mutuus.common.security.csrf-enabled=true"
})
class SecurityHeadersCsrfIntegrationTest {

    @LocalServerPort
    int port;

    @Test
    void 하드닝_활성시_응답에_보안헤더가_실린다() {
        RestClient client = RestClient.create();
        Map<String, String> h = client.get().uri(base() + "/api/public/whoami")
                .exchange((req, res) -> Map.of(
                        "referrer", String.valueOf(res.getHeaders().getFirst("Referrer-Policy")),
                        "permissions", String.valueOf(res.getHeaders().getFirst("Permissions-Policy"))));
        assertThat(h.get("referrer")).isEqualTo("no-referrer");
        assertThat(h.get("permissions")).contains("geolocation=()");
    }

    @Test
    void CSRF_활성시_토큰없는_POST는_403() {
        RestClient client = RestClient.create();
        int status = client.post().uri(base() + "/demo/board/jpa") // permit-all 이지만 CSRF 토큰 없음
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "x", "content", "y", "author", "z"))
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(403);
    }

    private String base() {
        return "http://localhost:" + port;
    }
}
