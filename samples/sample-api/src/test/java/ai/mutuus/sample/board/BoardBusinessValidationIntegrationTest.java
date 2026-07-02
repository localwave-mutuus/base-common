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
 * 게시판 <b>비즈니스(의미) 검증</b> 통합 테스트 — JPA/jOOQ/Spring Data JDBC 세 스택 공통으로 동일 규칙을 실서버로 확인한다.
 * <ul>
 *   <li>댓글은 좋아요 선행 필수 → 미선행 시 422({@code COMMENT_REQUIRES_LIKE})</li>
 *   <li>좋아요 멱등 → 재요청해도 카운트 불변</li>
 *   <li>같은 작성자 동일 제목 → 409({@code DUPLICATE_TITLE})</li>
 *   <li>공지([공지]) 비관리자 → 403({@code NOTICE_NOT_ALLOWED})</li>
 *   <li>없는 글 대상 → 404({@code POST_NOT_FOUND})</li>
 * </ul>
 * H2 는 대소문자 무시 식별자로 세 스택이 같은 board_post/like/comment 를 공유한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:board-it;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
class BoardBusinessValidationIntegrationTest {

    @LocalServerPort
    int port;

    @ParameterizedTest
    @ValueSource(strings = {"jpa", "jooq", "jdbc"})
    void 좋아요_댓글_제목중복_공지_존재_규칙이_스택마다_동일하게_적용된다(String stack) {
        RestClient client = RestClient.create();
        String base = "http://localhost:" + port + "/demo/board/" + stack;
        String title = "biz-" + stack; // 스택별 고유 제목(공유 테이블 중복 회피)

        // 1) 게시글 생성(200) + id 추출
        Map<?, ?> created = client.post().uri(base).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", title, "content", "body", "author", "alice"))
                .retrieve().body(Map.class);
        long id = ((Number) ((Map<?, ?>) created.get("data")).get("id")).longValue();

        // 2) 좋아요 없이 댓글 → 422
        assertThat(status(client, base + "/" + id + "/comments",
                Map.of("author", "bob", "content", "hi"))).isEqualTo(422);

        // 3) bob 좋아요 → 200, 재요청 멱등(카운트 1 유지)
        assertThat(likeCount(client, base + "/" + id + "/likes", "bob")).isEqualTo(1);
        assertThat(likeCount(client, base + "/" + id + "/likes", "bob")).isEqualTo(1);

        // 4) 좋아요 후 댓글 → 200
        assertThat(status(client, base + "/" + id + "/comments",
                Map.of("author", "bob", "content", "nice"))).isEqualTo(200);

        // 5) 같은 작성자 동일 제목 → 409
        assertThat(status(client, base,
                Map.of("title", title, "content", "x", "author", "alice"))).isEqualTo(409);

        // 6) 공지 비관리자 → 403
        assertThat(status(client, base,
                Map.of("title", "[공지]" + stack, "content", "x", "author", "alice"))).isEqualTo(403);

        // 7) 없는 글 좋아요 → 404
        assertThat(status(client, base + "/999999/likes", Map.of("author", "z"))).isEqualTo(404);

        // 8) 자식(좋아요+댓글)이 있는 글 삭제 → 200 (FK on delete cascade 로 자식 함께 정리, FK 위반 없음)
        int deleteStatus = client.delete().uri(base + "/" + id)
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(deleteStatus).isEqualTo(200);
        // 삭제 후 조회 → 404
        int getStatus = client.get().uri(base + "/" + id)
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(getStatus).isEqualTo(404);
    }

    /** POST 후 HTTP 상태코드만(오류에도 예외 없이). */
    private static int status(RestClient client, String url, Object body) {
        return client.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((req, res) -> res.getStatusCode().value());
    }

    @SuppressWarnings("unchecked")
    private static long likeCount(RestClient client, String url, String author) {
        Map<String, Object> res = client.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("author", author)).retrieve().body(Map.class);
        Map<String, Object> data = (Map<String, Object>) res.get("data");
        return ((Number) data.get("likeCount")).longValue();
    }
}
