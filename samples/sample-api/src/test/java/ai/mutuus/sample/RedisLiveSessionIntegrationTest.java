package ai.mutuus.sample;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import ai.mutuus.sample.support.LiveInfra;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이미 떠 있는 <b>로컬 실 Redis</b>(메모리 {@code local-infra-db-redis} 좌표)를 대상으로 세션
 * 컨벤션을 검증한다. Testcontainers 변형({@link RedisSessionIntegrationTest})은 Docker 가 있어야만
 * 돌지만, 이 테스트는 <b>실 Redis 에 자격증명으로 접속 가능할 때만</b> 돌고 아니면 자동 skip 된다
 * ({@link LiveInfra#redisReachable()}).
 *
 * <p>네임스페이스 컨벤션에 더해 <b>타임아웃 컨벤션</b>까지 본다: {@code mutuus.common.session.timeout}
 * 이 세션의 max-inactive-interval 로 반영되는지 확인한다(Boot 기본 30m 이 아니라 지정한 15m).
 * 공유 Redis 를 더럽히지 않도록 {@code demo:session:*} 키는 테스트 종료 시 정리한다.
 */
@SpringBootTest
@EnabledIf(value = "ai.mutuus.sample.support.LiveInfra#redisReachable",
        disabledReason = "로컬 실 Redis(기본 localhost:16010, cain/eva) 에 접속할 수 없음")
@TestPropertySource(properties = {
        "mutuus.common.session.namespace=demo:session",
        "mutuus.common.session.timeout=15m"
})
@Import(RedisLiveSessionIntegrationTest.TestSecurityConfig.class)
class RedisLiveSessionIntegrationTest {

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> LiveInfra.REDIS_HOST);
        registry.add("spring.data.redis.port", () -> LiveInfra.REDIS_PORT);
        registry.add("spring.data.redis.username", () -> LiveInfra.REDIS_USERNAME);
        registry.add("spring.data.redis.password", () -> LiveInfra.REDIS_PASSWORD);
    }

    @Autowired
    @SuppressWarnings("rawtypes")
    SessionRepository sessionRepository;

    @Autowired
    StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanup() {
        Set<String> keys = redisTemplate.keys("demo:session:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void 실_Redis에_저장_조회되고_네임스페이스와_타임아웃_컨벤션이_적용된다() {
        Session session = sessionRepository.createSession();
        session.setAttribute("user", "u-sess");
        sessionRepository.save(session);

        Session loaded = sessionRepository.findById(session.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.<String>getAttribute("user")).isEqualTo("u-sess");

        // 네임스페이스 컨벤션: 라이브러리 커스터마이저가 지정한 접두사로 키가 저장돼야 한다.
        assertThat(redisTemplate.keys("demo:session:*")).isNotEmpty();

        // 타임아웃 컨벤션: 지정한 15m 가 세션 비활성 유지시간으로 반영돼야 한다(Boot 기본 30m 아님).
        assertThat(loaded.getMaxInactiveInterval()).isEqualTo(Duration.ofMinutes(15));
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
