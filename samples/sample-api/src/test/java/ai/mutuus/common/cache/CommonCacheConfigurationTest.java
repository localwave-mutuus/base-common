package ai.mutuus.common.cache;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CommonCacheAutoConfiguration#buildCacheConfiguration} 단위 테스트 — Redis 연결 없이
 * 컨벤션(키 프리픽스·TTL·null 캐싱 비활성)이 올바르게 조립되는지 검증한다. 라이브러리와 같은
 * 패키지(split-package)로 두어 package-private 정적 메서드에 접근한다.
 */
class CommonCacheConfigurationTest {

    @Test
    void 프리픽스_미지정시_serviceName_기반_기본_프리픽스를_쓴다() {
        CommonCacheProperties props = new CommonCacheProperties();
        props.setTtl(Duration.ofMinutes(7));

        RedisCacheConfiguration config =
                CommonCacheAutoConfiguration.buildCacheConfiguration(props, "orders-api");

        assertThat(config.getKeyPrefixFor("demo")).isEqualTo("orders-api:cache:demo::");
        assertThat(config.getTtlFunction().getTimeToLive("k", "v")).isEqualTo(Duration.ofMinutes(7));
        assertThat(config.getAllowCacheNullValues()).isFalse(); // 기본 null 캐싱 비활성
    }

    @Test
    void 프리픽스_지정시_그_값을_쓰고_null캐싱_옵션이_반영된다() {
        CommonCacheProperties props = new CommonCacheProperties();
        props.setKeyPrefix("custom:");
        props.setCacheNullValues(true);

        RedisCacheConfiguration config =
                CommonCacheAutoConfiguration.buildCacheConfiguration(props, "orders-api");

        assertThat(config.getKeyPrefixFor("demo")).isEqualTo("custom:demo::");
        assertThat(config.getAllowCacheNullValues()).isTrue();
    }
}
