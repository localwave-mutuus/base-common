package ai.mutuus.sample.demo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 캐시 컨벤션 데모용 서비스. {@code @Cacheable} 이 붙은 메서드는 라이브러리
 * {@code CommonCacheAutoConfiguration}(캐시 opt-in)이 켜져 있고 Redis 가 연결되면
 * <b>같은 키의 재호출 시 재계산 없이 캐시 값을 반환</b>한다(캐시 미활성 시엔 매번 재계산).
 */
@Service
public class CacheDemoService {

    /** 호출마다 새 UUID 를 만든다 — 캐시가 동작하면 같은 key 의 2차 호출은 1차 값이 그대로 반환된다. */
    @Cacheable(cacheNames = "demo", key = "#key")
    public Map<String, Object> compute(String key) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("key", key);
        value.put("value", UUID.randomUUID().toString());
        value.put("computedAtNano", System.nanoTime());
        return value;
    }
}
