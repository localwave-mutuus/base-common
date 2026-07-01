package ai.mutuus.common.cache;

import ai.mutuus.common.config.CommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

/**
 * 공통 캐시 자동 구성(<b>기본 OFF, opt-in</b>). Redis 캐시(spring-data-redis)와 Boot 캐시 자동구성이
 * classpath 에 있고 {@code mutuus.common.cache.enabled=true} 일 때만 동작한다.
 * <p>두 가지를 한다: (1) {@code @EnableCaching} 으로 캐싱을 켜고(Boot 는 기본적으로 캐싱을 켜지 않음),
 * (2) 사용자 정의 {@link RedisCacheConfiguration} 빈으로 컨벤션 기본값 — 기본 TTL, 키 프리픽스
 * {@code <service-name>:cache:}(서비스 간 키 충돌 방지), JSON 값 직렬화 — 을 주입한다. Boot 의 Redis
 * 캐시 자동구성이 이 빈을 {@code cacheDefaults} 로 채택해 {@link RedisCacheManager} 를 만든다.
 * <p>실제 캐싱은 소비 서비스가 {@code @Cacheable} 등을 붙인 메서드에서만 일어난다. 소비자가 자체
 * {@link RedisCacheConfiguration} 또는 {@link CacheManager} 빈을 정의하면 그 값이 우선한다.
 */
@AutoConfiguration
@ConditionalOnClass(value = {CacheManager.class, RedisCacheManager.class},
        name = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@ConditionalOnProperty(prefix = "mutuus.common.cache", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CommonCacheProperties.class)
@EnableCaching
public class CommonCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisCacheConfiguration.class)
    RedisCacheConfiguration commonCacheConfiguration(CommonCacheProperties props,
                                                     ObjectProvider<CommonProperties> commonProperties) {
        String serviceName = commonProperties.getIfAvailable(CommonProperties::new).getServiceName();
        return buildCacheConfiguration(props, serviceName);
    }

    /**
     * 컨벤션 {@link RedisCacheConfiguration} 를 만든다(Redis 연결 없이 순수 계산 — 단위 테스트 가능).
     * 프리픽스 미지정 시 {@code <service-name>:cache:} 를 쓴다.
     */
    static RedisCacheConfiguration buildCacheConfiguration(CommonCacheProperties props, String serviceName) {
        String prefix = StringUtils.hasText(props.getKeyPrefix())
                ? props.getKeyPrefix() : serviceName + ":cache:";
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(props.getTtl())
                .prefixCacheNameWith(prefix)
                .serializeValuesWith(SerializationPair.fromSerializer(RedisSerializer.json()));
        if (!props.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        return config;
    }
}
