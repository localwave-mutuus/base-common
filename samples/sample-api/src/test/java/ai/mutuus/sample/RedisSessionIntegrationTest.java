package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소비 서비스가 Spring Session Redis 를 얹기만 하면, 라이브러리의 세션 컨벤션(서비스명 기반
 * 키 네임스페이스)이 적용된 채 실제 Redis 에 세션이 저장/조회되는지 검증한다.
 * <p>네임스페이스를 {@code demo:session} 으로 지정하고, 저장 후 Redis 키가 그 접두사로 생기는지
 * 확인해 라이브러리 커스터마이저가 실제로 반영됐음을 증명한다. <b>Docker 없으면 자동 skip</b>.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "mutuus.common.session.namespace=demo:session")
@Import(RedisSessionIntegrationTest.TestSecurityConfig.class)
class RedisSessionIntegrationTest {

    @Container
    @ServiceConnection("redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Autowired
    @SuppressWarnings("rawtypes")
    SessionRepository sessionRepository;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void 세션을_Redis에_저장하고_조회하며_네임스페이스_컨벤션이_적용된다() {
        Session session = sessionRepository.createSession();
        session.setAttribute("user", "u-sess");
        sessionRepository.save(session);

        Session loaded = sessionRepository.findById(session.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.<String>getAttribute("user")).isEqualTo("u-sess");

        // 라이브러리 커스터마이저가 지정한 네임스페이스로 키가 저장돼야 한다(Boot 기본은 spring:session).
        assertThat(redisTemplate.keys("demo:session:*")).isNotEmpty();
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
