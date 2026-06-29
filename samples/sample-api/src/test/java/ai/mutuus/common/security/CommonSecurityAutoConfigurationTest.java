package ai.mutuus.common.security;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 보안 자동구성의 핵심 커스텀 로직(JWT roles 클레임 → ROLE_* 권한 매핑)을 검증한다.
 * <p>SecurityFilterChain 빈 자체는 HttpSecurity 전체 인프라가 필요하므로
 * 풀 @SpringBootTest 슬라이스에서 검증하고, 여기서는 변환 로직만 단위 검증한다.
 */
class CommonSecurityAutoConfigurationTest {

    private final CommonSecurityAutoConfiguration config = new CommonSecurityAutoConfiguration();

    @Test
    void roles_클레임을_authorityPrefix_붙인_권한으로_변환한다() {
        var props = new CommonSecurityProperties();   // 기본값: rolesClaim=roles, prefix=ROLE_
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter(props);
        Jwt jwt = jwtWithClaim("roles", List.of("admin", "user"));

        // 우리 rolesConverter가 만든 권한만 검증 (Spring Security 7이 자동 추가하는 FACTOR_* 제외)
        assertThat(roleAuthorities(converter, jwt)).containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    void roles_클레임이_없으면_role_권한은_비어있다() {
        var props = new CommonSecurityProperties();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter(props);
        Jwt jwt = jwtWithClaim("other", List.of("x"));

        assertThat(roleAuthorities(converter, jwt)).isEmpty();
    }

    @Test
    void rolesClaim과_authorityPrefix는_설정으로_바꿀_수_있다() {
        var props = new CommonSecurityProperties();
        props.setRolesClaim("authorities");
        props.setAuthorityPrefix("SCOPE_");
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter(props);
        Jwt jwt = jwtWithClaim("authorities", List.of("read"));

        assertThat(roleAuthorities(converter, jwt)).containsExactly("SCOPE_read");
    }

    /**
     * 변환된 권한 중 프레임워크가 자동 부여하는 인증 팩터 권한(FACTOR_*)을 제외한,
     * 즉 우리 커스텀 rolesConverter의 산출물만 추려낸다.
     */
    private List<String> roleAuthorities(JwtAuthenticationConverter converter, Jwt jwt) {
        return converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("FACTOR_"))
                .toList();
    }

    private Jwt jwtWithClaim(String name, Object value) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(name, value)
                .build();
    }
}
