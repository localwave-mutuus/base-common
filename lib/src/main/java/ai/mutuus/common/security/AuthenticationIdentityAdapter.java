package ai.mutuus.common.security;

import java.util.Optional;
import ai.mutuus.common.core.VerifiedIdentityContext.Identity;
import org.springframework.security.core.Authentication;

/** 소비 서비스 인증 계약으로 확인한 불변 계정 ID/realm만 반환한다. getName이나 인입 헤더는 ID 계약이 아니다. */
@FunctionalInterface
public interface AuthenticationIdentityAdapter {
    Optional<Identity> resolve(Authentication authentication);
}
