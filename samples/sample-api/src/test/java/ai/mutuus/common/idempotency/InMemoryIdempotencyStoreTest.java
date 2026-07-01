package ai.mutuus.common.idempotency;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
