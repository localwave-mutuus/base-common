package ai.mutuus.sample.demo;

import java.time.Instant;
import java.util.List;

import ai.mutuus.common.security.CommonJwtValidators;
import ai.mutuus.common.security.CommonSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/**
 * 데모 전용 JwtDecoder — 속성 {@code mutuus.sample.mock-jwt=true} 일 때만 등록된다(기본 OFF).
 * <p>실제 IdP 없이 보안 경로를 시연한다. {@code Authorization: Bearer <name>} 의 name 을 주체(subject)로,
 * 권한을 USER 로 삼는다. 예: {@code Bearer alice} → 주체 alice.
 * <p><b>audience 하드닝 시연</b>: 토큰을 {@code <name>~<aud>} 로 주면 그 aud 를 담는다(예 {@code alice~other}).
 * (구분자 {@code ~} 는 RFC 6750 Bearer 토큰 허용문자라 필터 단계에서 malformed 로 걸리지 않고 디코더까지 도달한다.)
 * {@code mutuus.common.security.audiences} 가 설정된 경우 {@link CommonJwtValidators} 로 검증하여, 허용되지 않은
 * aud 는 {@link JwtValidationException} 으로 거부한다(자원 서버가 401 → 진입점이 {@code security.jwt.rejected} 기록).
 * {@code ~} 가 없으면 설정된 허용 audience 를 담아 통과시킨다.
 * <p>기본 OFF 라 통합테스트(자체 mock JwtDecoder 제공)와 빈 충돌이 없다. Boot 의 JWK 기반 JwtDecoder 자동구성은
 * {@code @ConditionalOnMissingBean} 이라 이 빈이 있으면 비활성된다.
 */
@Configuration
@ConditionalOnProperty(prefix = "mutuus.sample", name = "mock-jwt", havingValue = "true")
public class DemoSecurityConfig {

    @Bean
    JwtDecoder demoJwtDecoder(CommonSecurityProperties props) {
        OAuth2TokenValidator<Jwt> validator = CommonJwtValidators.build(props.getAudiences(), props.getIssuer());
        return token -> {
            if (token == null || token.isBlank()) {
                throw new BadJwtException("demo: 빈 토큰");
            }
            String subject = token;
            List<String> audience;
            int sep = token.indexOf('~');
            if (sep >= 0) {                                    // <name>~<aud> → 지정 aud
                subject = token.substring(0, sep);
                audience = List.of(token.substring(sep + 1));
            } else {                                           // 기본: 설정된 허용 audience 를 담아 통과
                audience = List.copyOf(props.getAudiences());
            }
            Instant now = Instant.now();
            Jwt jwt = Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(subject)
                    .audience(audience)
                    .claim("roles", List.of("USER"))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .build();
            OAuth2TokenValidatorResult result = validator.validate(jwt);
            if (result.hasErrors()) {
                OAuth2Error err = result.getErrors().iterator().next();
                throw new JwtValidationException(err.getDescription(), result.getErrors());
            }
            return jwt;
        };
    }
}
