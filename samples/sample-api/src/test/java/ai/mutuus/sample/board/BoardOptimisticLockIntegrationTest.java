package ai.mutuus.sample.board;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시판 <b>낙관적 락(@Version)</b> 통합 테스트 — JPA/jOOQ/Spring Data JDBC 세 스택 공통으로 실서버 확인한다.
 * <ul>
 *   <li>생성 직후 {@code version} 은 0, 올바른 version 으로 수정 시 200 + version 증가</li>
 *   <li>내가 읽은 version 이 이미 낡았으면(그 사이 남이 수정) 409({@code STALE_UPDATE})</li>
 *   <li>version 미지정(null) 이면 버전 검사 없이 갱신(하위호환) → 200</li>
 * </ul>
 * H2 는 대소문자 무시 식별자로 세 스택이 같은 board_post 를 공유한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:board-it;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
class BoardOptimisticLockIntegrationTest {

    @LocalServerPort
    int port;

    @ParameterizedTest
    @ValueSource(strings = {"jpa", "jooq", "jdbc"})
    void 낙관적_락은_낡은_version_수정을_409로_막는다(String stack) {
        RestClient client = RestClient.create();
        String base = "http://localhost:" + port + "/demo/board/" + stack;
        String title = "lock-" + stack; // 스택별 고유 제목(공유 테이블 중복 회피)

        // 1) 생성 → version 0
        Map<?, ?> created = post(client, base, Map.of("title", title, "content", "v0", "author", "carol"));
        long id = ((Number) created.get("id")).longValue();
        long v0 = ((Number) created.get("version")).longValue();
        assertThat(v0).isZero();

        // 2) 올바른 version(0)으로 수정 → 200, version 1 로 증가
        Map<?, ?> updated = putOk(client, base + "/" + id + "?version=" + v0,
                Map.of("title", title, "content", "v1", "author", "carol"));
        long v1 = ((Number) updated.get("version")).longValue();
        assertThat(v1).isEqualTo(1L);

        // 3) 낡은 version(0)으로 재수정 → 409(STALE_UPDATE)
        assertThat(putStatus(client, base + "/" + id + "?version=" + v0,
                Map.of("title", title, "content", "stale", "author", "carol"))).isEqualTo(409);

        // 4) version 미지정 → 검사 없이 갱신(하위호환) → 200
        assertThat(putStatus(client, base + "/" + id,
                Map.of("title", title, "content", "no-version", "author", "carol"))).isEqualTo(200);
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> post(RestClient client, String url, Object body) {
        Map<String, Object> res = client.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(Map.class);
        return (Map<?, ?>) res.get("data");
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> putOk(RestClient client, String url, Object body) {
        Map<String, Object> res = client.put().uri(url).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(Map.class);
        return (Map<?, ?>) res.get("data");
    }

    /** PUT 후 HTTP 상태코드만(오류에도 예외 없이). */
    private static int putStatus(RestClient client, String url, Object body) {
        return client.put().uri(url).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((req, res) -> res.getStatusCode().value());
    }
}
