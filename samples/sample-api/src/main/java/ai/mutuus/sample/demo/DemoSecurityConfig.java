package ai.mutuus.sample.demo;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * 데모 전용 JwtDecoder — 속성 {@code mutuus.sample.mock-jwt=true} 일 때만 등록된다(기본 OFF).
 * <p>실제 IdP 없이 보안 "인증 성공" 경로를 시연하기 위해, 전달된 Bearer 토큰 문자열을
 * 그대로 subject 로 삼는 가짜 디코더다. 즉 {@code Authorization: Bearer alice} 로 호출하면
 * 인증 주체가 {@code alice} 가 되어 {@code AuthenticatedUserContextFilter} 가
 * {@code X-User-Id=alice} 를 TraceContext/MDC 에 반영한다.
 * <p>기본 OFF 라 통합테스트(자체 mock JwtDecoder 제공)와 빈 충돌이 없다. 실행 구성
 * (.vscode/launch.json)에서 {@code --mutuus.sample.mock-jwt=true} 로 켠다. Boot 의 JWK 기반
 * JwtDecoder 자동구성은 {@code @ConditionalOnMissingBean} 이라 이 빈이 있으면 비활성된다.
 */
@Configuration
@ConditionalOnProperty(prefix = "mutuus.sample", name = "mock-jwt", havingValue = "true")
public class DemoSecurityConfig {

    @Bean
    JwtDecoder demoJwtDecoder() {
        return token -> {
            if (token == null || token.isBlank()) {
                throw new BadJwtException("demo: 빈 토큰");
            }
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(token) // 데모 편의: 토큰 문자열 = 인증 주체(subject)
                    .claim("roles", List.of("USER"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        };
    }
}
