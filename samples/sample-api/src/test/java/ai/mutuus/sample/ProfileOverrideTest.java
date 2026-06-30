package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 환경별 설정파일(application-&lt;profile&gt;.yml) 로딩이 동작함을 검증한다(외부 인프라 불필요).
 * <p>{@code stage} 프로파일을 활성화하면 {@code application-stage.yml} 이 로딩되어
 * {@code application.yml} 의 기본값({@code demo.environment=unknown})을 {@code stage} 로
 * 덮어쓴다 — 이는 라이브러리 기능이 아니라 Spring Boot 표준 프로파일 기능이다.
 * <p>stage/production 프로파일 파일은 DB 접속값을 두지 않아(시크릿은 환경변수 주입) 테스트는
 * 기본 H2 로 기동되므로 외부 인프라 없이도 돈다.
 */
@SpringBootTest
@ActiveProfiles("stage")
@Import(ProfileOverrideTest.TestSecurityConfig.class)
class ProfileOverrideTest {

    @Autowired
    Environment env;

    @Test
    void stage_프로파일이_활성화되면_환경별_설정이_로딩되어_공통_기본값을_덮어쓴다() {
        assertThat(env.getActiveProfiles()).containsExactly("stage");
        // application.yml 기본값 "unknown" → application-stage.yml 이 "stage" 로 덮어씀
        assertThat(env.getProperty("demo.environment")).isEqualTo("stage");
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
