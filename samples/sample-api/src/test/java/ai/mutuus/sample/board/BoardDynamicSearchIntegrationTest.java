package ai.mutuus.sample.board;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글 <b>동적 검색</b>(jOOQ 특화) 통합 테스트 — 동적 WHERE(LIKE·IN·from~to)와 동적 SELECT(fields)를 실서버로 검증한다.
 * <ul>
 *   <li>keyword LIKE — 제목/작성자 부분일치</li>
 *   <li>authors IN — 작성자 목록 정확일치</li>
 *   <li>createdFrom~To 범위 — 상한을 과거로 주면 0건(범위가 실제 적용됨)</li>
 *   <li>fields 동적 SELECT — 요청한 컬럼만 응답(sparse fieldset)</li>
 *   <li>허용목록 밖 필드 → 400, 범위 역전(from&gt;to) → 422</li>
 * </ul>
 * 공유 H2(board-it)를 다른 테스트와 함께 쓰므로 고유 마커(dynq-*)로 대상 행을 격리한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:board-it;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
class BoardDynamicSearchIntegrationTest {

    private static final String MARKER = "dynq"; // 이 테스트가 만든 행만 걸러내는 제목 마커

    @LocalServerPort
    int port;

    RestClient client;
    String base;

    @BeforeEach
    void setUp() {
        client = RestClient.create();
        base = "http://localhost:" + port + "/demo/board/jooq";
        // 시드: 서로 다른 작성자 3명, 제목엔 공통 마커
        create(MARKER + "-1", "dyn-alice");
        create(MARKER + "-2", "dyn-bob");
        create(MARKER + "-3", "dyn-carol");
    }

    @Test
    void keyword_LIKE_는_제목부분일치로_대상행을_찾는다() {
        Map<?, ?> data = search(Map.of("keyword", MARKER));
        assertThat(total(data)).isEqualTo(3);
        assertThat(content(data)).allSatisfy(row ->
                assertThat((String) row.get("title")).contains(MARKER));
    }

    @Test
    void authors_IN_은_지정_작성자만_반환한다() {
        Map<?, ?> data = search(Map.of("keyword", MARKER, "authors", List.of("dyn-alice", "dyn-bob")));
        assertThat(total(data)).isEqualTo(2);
        assertThat(content(data)).allSatisfy(row ->
                assertThat((String) row.get("author")).isIn("dyn-alice", "dyn-bob"));
    }

    @Test
    void createdTo_상한을_과거로_주면_범위밖이라_0건이다() {
        Map<?, ?> data = search(Map.of(
                "keyword", MARKER,
                "createdTo", Instant.parse("2000-01-01T00:00:00Z")));
        assertThat(total(data)).isZero();
    }

    @Test
    void createdFrom_과거_createdTo_미래_범위는_전부_포함한다() {
        Map<?, ?> data = search(Map.of(
                "keyword", MARKER,
                "createdFrom", Instant.parse("2000-01-01T00:00:00Z"),
                "createdTo", Instant.parse("2999-01-01T00:00:00Z")));
        assertThat(total(data)).isEqualTo(3);
    }

    @Test
    void fields_동적SELECT_는_요청한_컬럼만_반환한다() {
        Map<?, ?> data = search(Map.of("keyword", MARKER, "fields", List.of("id", "title")));
        assertThat(content(data)).allSatisfy(row ->
                assertThat(row.keySet()).containsExactlyInAnyOrder("id", "title"));
    }

    @Test
    void fields_생략시_기본_전체컬럼이_반환된다() {
        Map<?, ?> data = search(Map.of("keyword", MARKER));
        assertThat(content(data)).allSatisfy(row -> assertThat(row.keySet())
                .contains("id", "title", "content", "author", "createdAt", "updatedAt", "version"));
    }

    @Test
    void 허용목록_밖의_필드는_400() {
        int status = client.post().uri(base + "/search").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fields", List.of("id", "password"))) // password 는 허용목록 밖
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(400);
    }

    @Test
    void 범위_역전_from이_to보다_늦으면_422() {
        int status = client.post().uri(base + "/search").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "createdFrom", Instant.parse("2999-01-01T00:00:00Z"),
                        "createdTo", Instant.parse("2000-01-01T00:00:00Z")))
                .exchange((req, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(422);
    }

    // ----- helpers -----

    private void create(String title, String author) {
        // 공유 H2(board-it)는 클래스 내 테스트 간 유지되므로, 첫 테스트가 시드하면 이후엔 중복(409) → 멱등 무시.
        client.post().uri(base).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", title, "content", "c", "author", author))
                .exchange((req, res) -> res.getStatusCode().value());
    }

    private Map<?, ?> search(Map<String, Object> criteria) {
        Map<?, ?> res = client.post().uri(base + "/search").contentType(MediaType.APPLICATION_JSON)
                .body(criteria).retrieve().body(Map.class);
        return (Map<?, ?>) res.get("data"); // PageResponse
    }

    private static long total(Map<?, ?> pageData) {
        return ((Number) pageData.get("totalElements")).longValue();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> content(Map<?, ?> pageData) {
        return (List<Map<String, Object>>) pageData.get("content");
    }
}
