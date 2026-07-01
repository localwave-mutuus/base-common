package ai.mutuus.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 #17(멱등성) 검증 — 실서버(RANDOM_PORT)로 {@code /demo/idem/echo}(호출마다 새 UUID) 를
 * 같은/다른 {@code Idempotency-Key} 로 호출해 재방(replay) 동작을 확인한다.
 * <ul>
 *   <li>같은 키 → 2차는 재처리 없이 1차 응답 재방(본문 동일 + {@code Idempotent-Replayed: true})</li>
 *   <li>다른 키 → 각각 새로 처리(본문 상이)</li>
 *   <li>키 없음 → 매번 새로 처리(멱등 미적용)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdempotencyIntegrationTest {

    @LocalServerPort
    int port;

    private RestClient client() {
        return RestClient.create();
    }

    private ResponseEntity<String> post(String key) {
        RestClient.RequestBodySpec spec = client().post().uri("http://localhost:" + port + "/demo/idem/echo");
        if (key != null) {
            spec = spec.header("Idempotency-Key", key);
        }
        return spec.retrieve().toEntity(String.class);
    }

    @Test
    void 같은_키의_중복_POST는_첫_응답을_재방한다() {
        String key = "itest-same-key";
        ResponseEntity<String> first = post(key);
        ResponseEntity<String> second = post(key);

        assertThat(first.getBody()).isEqualTo(second.getBody());            // 본문 동일(재처리 없음)
        assertThat(first.getHeaders().getFirst("Idempotent-Replayed")).isNull(); // 1차는 실제 처리
        assertThat(second.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true"); // 2차는 재방
    }

    @Test
    void 다른_키는_각각_새로_처리된다() {
        ResponseEntity<String> a = post("itest-key-a");
        ResponseEntity<String> b = post("itest-key-b");

        assertThat(a.getBody()).isNotEqualTo(b.getBody());
        assertThat(b.getHeaders().getFirst("Idempotent-Replayed")).isNull();
    }

    @Test
    void 키가_없으면_멱등_처리되지_않는다() {
        ResponseEntity<String> a = post(null);
        ResponseEntity<String> b = post(null);

        assertThat(a.getBody()).isNotEqualTo(b.getBody());
        assertThat(a.getHeaders().getFirst("Idempotent-Replayed")).isNull();
        assertThat(b.getHeaders().getFirst("Idempotent-Replayed")).isNull();
    }
}
