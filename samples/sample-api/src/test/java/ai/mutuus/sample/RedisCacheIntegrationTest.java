package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import ai.mutuus.sample.demo.CacheDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
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

    @Container
    @ServiceConnection("redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Autowired
    CacheDemoService cacheDemoService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void 같은_키_재호출은_캐시를_반환하고_프리픽스_컨벤션이_적용된다() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        String key = "itest-key";
        Map<String, Object> first = cacheDemoService.compute(key);
        Map<String, Object> second = cacheDemoService.compute(key); // 캐시 히트 → 재계산 없음

        assertThat(second).isEqualTo(first);
        // 라이브러리 컨벤션 프리픽스로 키가 저장돼야 한다(<prefix>demo::<key>).
        assertThat(redisTemplate.keys("itest:cache:demo::*")).isNotEmpty();
    }

    @TestConfiguration
    static class TestSecurityConfig {
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
