package ai.mutuus.common.idempotency;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link InMemoryIdempotencyStore} 단위 테스트 — 라이브러리와 같은 패키지(split-package)로 둔다.
 * reserve(예약/중복)·find(처리중/완료/만료)·complete(완료 저장) 계약을 검증한다.
 */
class InMemoryIdempotencyStoreTest {

    private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();

    @Test
    void reserve는_첫_호출만_true_이후_중복은_false() {
        Duration ttl = Duration.ofMinutes(5);
        assertThat(store.reserve("k1", ttl)).isTrue();  // 첫 예약
        assertThat(store.reserve("k1", ttl)).isFalse(); // 중복
    }

    @Test
    void reserve_직후_find는_처리중_마커를_돌려준다() {
        store.reserve("k2", Duration.ofMinutes(5));
        IdempotencyRecord r = store.find("k2");
        assertThat(r).isNotNull();
        assertThat(r.completed()).isFalse();
    }

    @Test
    void complete_후_find는_완료_스냅샷을_돌려준다() {
        store.reserve("k3", Duration.ofMinutes(5));
        byte[] body = "{\"v\":1}".getBytes(StandardCharsets.UTF_8);
        store.complete("k3", IdempotencyRecord.completed(200, "application/json", body), Duration.ofMinutes(5));

        IdempotencyRecord r = store.find("k3");
        assertThat(r).isNotNull();
        assertThat(r.completed()).isTrue();
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.contentType()).isEqualTo("application/json");
        assertThat(r.body()).isEqualTo(body);
    }

    @Test
    void 없는_키는_find가_null() {
        assertThat(store.find("absent")).isNull();
    }

    @Test
    void TTL_만료된_예약은_재예약이_가능하다() {
        // ttl 0 → 즉시 만료. 다음 reserve 는 만료 항목을 교체하며 true 를 돌려준다.
        assertThat(store.reserve("k4", Duration.ZERO)).isTrue();
        assertThat(store.find("k4")).isNull();          // 만료 → 조회 시 없음
        assertThat(store.reserve("k4", Duration.ofMinutes(5))).isTrue(); // 재예약 가능
    }

    @Test
    void filter_exception_removes_in_progress_record_instead_of_caching_partial_response() {
        IdempotencyProperties props = new IdempotencyProperties();
        IdempotencyFilter filter = new IdempotencyFilter(store, props);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/idem/fail");
        request.addHeader("Idempotency-Key", "boom-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (req, res) -> {
                    throw new ServletException("boom");
                })).isInstanceOf(ServletException.class);

        assertThat(store.find("boom-key")).isNull();
    }

    @Test
    void filter_rejects_same_key_used_for_different_request_fingerprint() throws Exception {
        IdempotencyProperties props = new IdempotencyProperties();
        IdempotencyFilter filter = new IdempotencyFilter(store, props);

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/idem/a");
        first.addHeader("Idempotency-Key", "same-key");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, (req, res) -> res.getWriter().write("ok"));

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/idem/b");
        second.addHeader("Idempotency-Key", "same-key");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, (req, res) -> {
        });

        assertThat(secondResponse.getStatus()).isEqualTo(409);
        assertThat(secondResponse.getHeader("Idempotent-Replayed")).isEqualTo("fingerprint-mismatch");
    }
}
