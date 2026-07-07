package ai.mutuus.common.idempotency;

import java.time.Duration;

/**
 * 멱등 키 저장소. 라이브러리는 인메모리 기본 구현({@link InMemoryIdempotencyStore})을 제공하며,
 * <b>분산 환경에서는 소비 서비스가 Redis 등 공유 저장소 기반 구현을 빈으로 제공</b>해 대체한다
 * ({@code @ConditionalOnMissingBean}).
 */
public interface IdempotencyStore {

    /**
     * 키에 <b>처리 중(in-progress)</b> 마커를 원자적으로 등록한다.
     * @return 새로 등록했으면 {@code true}(첫 요청), 이미 있으면 {@code false}(중복).
     */
    boolean reserve(String key, Duration ttl);

    /** Reserve with a request fingerprint. Existing custom stores can ignore the fingerprint. */
    default boolean reserve(String key, Duration ttl, String fingerprint) {
        return reserve(key, ttl);
    }

    /** 저장된 레코드(처리 중 또는 완료). 없으면 {@code null}. */
    IdempotencyRecord find(String key);

    /** 처리 완료 응답을 저장한다(마커 → completed). */
    void complete(String key, IdempotencyRecord record, Duration ttl);

    /** Remove a reserved or completed record. Default no-op keeps existing custom implementations compatible. */
    default void remove(String key) {
    }
}
