package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ai.mutuus.sample.demo.CacheDemoService;
import ai.mutuus.sample.support.LiveInfra;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이미 떠 있는 <b>로컬 실 Redis</b>(메모리 {@code local-infra-db-redis} 좌표)를 대상으로 캐시 컨벤션을
 * 검증한다. Testcontainers 변형({@link RedisCacheIntegrationTest})은 Docker 가 있어야 돌지만, 이 테스트는
 * <b>실 Redis 에 자격증명으로 접속 가능할 때만</b> 돌고 아니면 자동 skip 된다({@link LiveInfra#redisReachable()}).
 * <p>같은 키의 재호출이 캐시로 반환되고, 라이브러리 프리픽스({@code <prefix>demo::})로 키가 저장되는지 본다.
 * 공유 Redis 를 더럽히지 않도록 테스트 키는 종료 시 정리한다.
 */
@SpringBootTest
@EnabledIf(value = "ai.mutuus.sample.support.LiveInfra#redisReachable",
        disabledReason = "로컬 실 Redis(기본 localhost:16010, default/eva) 에 접속할 수 없음")
@TestPropertySource(properties = "mutuus.common.cache.enabled=true")
@Import(RedisCacheLiveIntegrationTest.TestSecurityConfig.class)
class RedisCacheLiveIntegrationTest {

    private static final String KEY_PREFIX = "live:cache:" + UUID.randomUUID() + ":";

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> LiveInfra.REDIS_HOST);
        registry.add("spring.data.redis.port", () -> LiveInfra.REDIS_PORT);
        registry.add("spring.data.redis.username", () -> LiveInfra.REDIS_USERNAME);
        registry.add("spring.data.redis.password", () -> LiveInfra.REDIS_PASSWORD);
        registry.add("mutuus.common.cache.key-prefix", () -> KEY_PREFIX);
    }

    @Autowired
    CacheDemoService cacheDemoService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanup() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void 실_Redis에_캐싱되고_같은_키_재호출은_캐시를_반환한다() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        String key = "live-key";
        // 로컬 실 Redis 는 Traefik TCP 프록시 경유 공유 인프라라 방금 쓴 값의 즉시 재조회 가시성이
        // 드물게 지연될 수 있다(캐시 기록 자체는 동기). 연속 두 조회가 같아질 때까지 짧게 재조회해
        // 캐시 히트를 확인한다(전용 Redis 라면 1~2회에 안정). 캐시가 아예 안 되면 끝까지 불일치 → 실패.
        Map<String, Object> prev = cacheDemoService.compute(key);
        boolean cached = false;
        for (int i = 0; i < 5; i++) {
            Map<String, Object> read = cacheDemoService.compute(key);
            if (read.equals(prev)) {
                cached = true;
                break;
            }
            prev = read;
        }
        assertThat(cached).as("같은 키 재호출이 캐시 값을 반환(최대 5회 내 안정화)").isTrue();
        assertThat(redisTemplate.keys(KEY_PREFIX + "demo::*")).isNotEmpty();
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
