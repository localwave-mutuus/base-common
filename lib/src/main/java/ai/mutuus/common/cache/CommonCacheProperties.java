package ai.mutuus.common.cache;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 캐시 설정. {@code mutuus.common.cache.*}. <b>기본 OFF(opt-in)</b>.
 * <p>켜면 {@code @EnableCaching} 을 활성화하고 Redis {@code CacheManager} 에 컨벤션 기본값
 * (TTL·키 프리픽스·JSON 값 직렬화)을 주입한다. 캐싱은 소비 서비스가 {@code @Cacheable} 등을 붙인
 * 메서드에서만 실제로 동작하므로, 앱 동작을 바꾸는 opt-in 으로 둔다.
 */
@ConfigurationProperties(prefix = "mutuus.common.cache")
public class CommonCacheProperties {

    /** 공통 캐시 컨벤션 활성화 여부(기본 false). */
    private boolean enabled = false;

    /** 캐시 항목 기본 TTL. */
    private Duration ttl = Duration.ofMinutes(10);

    /** Redis 키 프리픽스. 미지정 시 {@code <service-name>:cache:} 를 사용(서비스 간 키 충돌 방지). */
    private String keyPrefix;

    /** null 값도 캐싱할지 여부(기본 false — null 캐싱 비활성). */
    private boolean cacheNullValues = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public void setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
    }
}
