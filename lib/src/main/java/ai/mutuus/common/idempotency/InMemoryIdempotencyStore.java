package ai.mutuus.common.idempotency;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link IdempotencyStore} 인메모리 기본 구현(단일 인스턴스용). TTL 만료를 함께 관리한다.
 * <p><b>분산 환경(다중 인스턴스)에서는 Redis 등 공유 저장소 구현으로 대체</b>해야 한다
 * (인메모리는 인스턴스 간 공유되지 않음).
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private record Entry(IdempotencyRecord record, long expiresAtMillis) {
    }

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

    @Override
    public boolean reserve(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        Entry candidate = new Entry(IdempotencyRecord.inProgress(), now + ttl.toMillis());
        while (true) {
            Entry prev = map.putIfAbsent(key, candidate);
            if (prev == null) {
                return true; // 새로 예약(첫 요청)
            }
            if (prev.expiresAtMillis() > now) {
                return false; // 유효한 기존 항목 → 중복
            }
            // 만료된 항목이면 원자적으로 교체 시도
            if (map.replace(key, prev, candidate)) {
                return true;
            }
            // 다른 스레드가 먼저 갱신 → 재시도
        }
    }

    @Override
    public IdempotencyRecord find(String key) {
        Entry e = map.get(key);
        if (e == null) {
            return null;
        }
        if (e.expiresAtMillis() <= System.currentTimeMillis()) {
            map.remove(key, e); // 만료 청소
            return null;
        }
        return e.record();
    }

    @Override
    public void complete(String key, IdempotencyRecord record, Duration ttl) {
        map.put(key, new Entry(record, System.currentTimeMillis() + ttl.toMillis()));
    }
}
