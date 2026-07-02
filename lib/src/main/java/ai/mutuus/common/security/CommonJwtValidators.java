package ai.mutuus.common.security;

import java.util.List;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.util.StringUtils;

/**
 * 공통 JWT 검증기 조립 — 기본(만료/nbf) + <b>issuer</b>(설정 시) + <b>audience</b>(설정 시)를 위임 검증기로 묶는다.
 * audience 검증은 <b>다른 서비스용으로 발급된 같은 발급자 토큰</b>을 막는 하드닝이다(자원 서버 기본은 aud 미검증).
 * 검증 실패는 {@link OAuth2Error}({@code invalid_token})로 표현되어 자원 서버가 401 로 응답하고, 진입점이
 * {@code security.jwt.rejected} 로 사유를 남긴다.
 */
public final class CommonJwtValidators {

    private CommonJwtValidators() {
    }

    /** audiences/issuer 설정을 반영한 위임 검증기. 둘 다 비어 있으면 기본(만료 등)만 적용한다. */
    public static OAuth2TokenValidator<Jwt> build(List<String> audiences, String issuer) {
        DelegatingOAuth2TokenValidator<Jwt> base =
                new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault());
        boolean hasIssuer = StringUtils.hasText(issuer);
        boolean hasAudience = audiences != null && !audiences.isEmpty();
        if (!hasIssuer && !hasAudience) {
            return base;
        }
        var validators = new java.util.ArrayList<OAuth2TokenValidator<Jwt>>();
        validators.add(base);
        if (hasIssuer) {
            validators.add(new JwtIssuerValidator(issuer));
        }
        if (hasAudience) {
            validators.add(audienceValidator(audiences));
        }
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    /** aud 클레임에 허용 audience 중 하나가 포함돼야 성공. */
    public static OAuth2TokenValidator<Jwt> audienceValidator(List<String> audiences) {
        return jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && audiences.stream().anyMatch(aud::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "The required audience (" + JwtClaimNames.AUD + ") is missing or not allowed",
                    null));
        };
    }
}
