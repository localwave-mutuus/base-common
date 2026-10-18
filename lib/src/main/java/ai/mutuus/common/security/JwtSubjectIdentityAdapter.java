package ai.mutuus.common.security;

import java.util.Optional;
import ai.mutuus.common.core.VerifiedIdentityContext.Identity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Spring 자원 서버가 검증한 회원 JWT의 subject만 사용한다. BO는 accountId 계약 adapter를 별도로 제공한다. */
public final class JwtSubjectIdentityAdapter implements AuthenticationIdentityAdapter {
    @Override
    public Optional<Identity> resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            return Optional.empty();
        }
        String subject = jwt.getToken().getSubject();
        return subject == null || subject.isBlank() ? Optional.empty()
                : Optional.of(new Identity(subject, "member", "authenticated"));
    }
}
