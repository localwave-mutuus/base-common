package ai.mutuus.sample;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.LocaleResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 라이브러리 자동구성이 Spring Boot 자동구성과 빈 충돌/중복을 일으키지 않는지 전수 점검.
 * <p>교차 관심사 타입은 컨텍스트에 정확히 1개만 존재해야 한다(중복 시 by-type 주입 모호성/오버라이드 예외).
 */
@SpringBootTest
@Import(ContextAuditTest.TestSecurityConfig.class)
class ContextAuditTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void MessageSource는_컨텍스트에_하나만_존재한다() {
        assertThat(ctx.getBeanNamesForType(MessageSource.class)).hasSize(1);
    }

    @Test
    void LocaleResolver는_컨텍스트에_하나만_존재한다() {
        assertThat(ctx.getBeanNamesForType(LocaleResolver.class)).hasSize(1);
    }

    @Test
    void SecurityFilterChain은_컨텍스트에_하나만_존재한다() {
        assertThat(ctx.getBeanNamesForType(SecurityFilterChain.class)).hasSize(1);
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
