package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ai.mutuus.sample.demo.CacheDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소비 서비스가 캐시 스타터+Redis 를 얹고 {@code mutuus.common.cache.enabled=true} 로 켜면, 라이브러리
 * 컨벤션(Redis {@link RedisCacheManager}, 키 프리픽스)이 적용된 채 {@code @Cacheable} 이 실제 Redis 에
 * 캐싱되는지 검증한다. <b>Docker 없으면 자동 skip</b>.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "mutuus.common.cache.enabled=true",
        "mutuus.common.cache.key-prefix=itest:cache:"
})
@Import(RedisCacheIntegrationTest.TestSecurityConfig.class)
class RedisCacheIntegrationTest {

    @Autowired
    CacheDemoService cacheDemoService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void 같은_키_재호출은_캐시를_반환하고_프리픽스_컨벤션이_적용된다() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        assertThat(AopUtils.isAopProxy(cacheDemoService)).as("Spring AOP 서비스 프록시").isTrue();
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
        RedisCache cache = (RedisCache) cacheManager.getCache("demo");
        assertThat(cache).isNotNull();
        assertThat(cache.getCacheConfiguration().getKeyPrefixFor("demo")).isEqualTo("itest:cache:demo::");

        String key = "itest-key-" + UUID.randomUUID();
        String redisKey = "itest:cache:demo::" + key;
        Map<String, Object> first = cacheDemoService.compute(key);
        Map<String, Object> second = cacheDemoService.compute(key); // 캐시 히트 → 재계산 없음
        // 두 compute 사이에 Redis 진단을 끼우지 않아 즉시 HIT 검증을 지연시키지 않는다.
        assertThat(second).isEqualTo(first);
        assertThat(redisTemplate.hasKey(redisKey)).as("첫 호출이 정확한 프리픽스 키에 저장됨").isTrue();
        assertThat(redisTemplate.getExpire(redisKey)).as("첫 저장 TTL이 양수").isPositive();
        assertThat(cache.get(key, Map.class)).as("Redis에서 역직렬화한 첫 저장값").isEqualTo(first);
    }

    @TestConfiguration
    static class TestSecurityConfig {
        // 컨테이너를 Spring context가 소유하여 캐시된 context보다 먼저 종료되지 않게 한다.
        @Bean
        @ServiceConnection("redis")
        GenericContainer<?> redis() {
            return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!"valid-token".equals(token)) {
                    throw new BadJwtException("invalid token");
                }
                return Jwt.withTokenValue(token).header("alg", "none").subject("user-1")
                        .claim("roles", List.of("USER"))
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            };
        }
    }
}
