package ai.mutuus.common.security;

import java.util.List;

import ai.mutuus.common.security.audit.SecurityAuditLogger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 보안 자동구성의 핵심 커스텀 로직(JWT roles 클레임 → ROLE_* 권한 매핑)을 검증한다.
 * <p>실제 보안 체인의 인증→사용자 문맥→예외 변환→인가 순서도 최소 웹 컨텍스트로 검증한다.
 */
class CommonSecurityAutoConfigurationTest {

    @Test
    void 검증사용자_필터는_bearer인증_이후_예외변환과_인가_이전에_배치한다() {
        new WebApplicationContextRunner()
                .withUserConfiguration(SecurityInfrastructure.class, CommonSecurityAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuthenticationIdentityAdapter.class);
                    assertThat(context.getBean(AuthenticationIdentityAdapter.class))
                            .isInstanceOf(JwtSubjectIdentityAdapter.class);
                    var filters = context.getBean(SecurityFilterChain.class).getFilters().stream()
                            .map(filter -> (Class<?>) filter.getClass()).toList();
                    int bearer = filters.indexOf(BearerTokenAuthenticationFilter.class);
                    int identity = filters.indexOf(AuthenticatedUserContextFilter.class);
                    int exceptionTranslation = filters.indexOf(ExceptionTranslationFilter.class);
                    int authorization = filters.indexOf(AuthorizationFilter.class);
                    assertThat(bearer).isGreaterThanOrEqualTo(0);
                    assertThat(identity).isGreaterThan(bearer);
                    // 403 핸들러도 검증 사용자 scope 안에서 실행되어야 한다.
                    assertThat(exceptionTranslation).isGreaterThan(identity);
                    assertThat(authorization).isGreaterThan(exceptionTranslation);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class SecurityInfrastructure {
        @Bean
        ai.mutuus.common.i18n.MessageResolver messageResolver() {
            return new ai.mutuus.common.i18n.MessageResolver(new org.springframework.context.support.StaticMessageSource());
        }
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token).header("alg", "RS256").subject("synthetic-member").build();
        }
    }
    private final CommonSecurityAutoConfiguration config = new CommonSecurityAutoConfiguration();

    @Test
    void roles_클레임을_authorityPrefix_붙인_권한으로_변환한다() {
        var props = new CommonSecurityProperties();   // 기본값: rolesClaim=roles, prefix=ROLE_
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter(props, auditProvider());
        Jwt jwt = jwtWithClaim("roles", List.of("admin", "user"));

        // 우리 rolesConverter가 만든 권한만 검증 (Spring Security 7이 자동 추가하는 FACTOR_* 제외)
        assertThat(roleAuthorities(converter, jwt)).containsExactlyInAnyOrder("ROLE_admin", "ROLE_user");
    }

    @Test
    void roles_클레임이_없으면_role_권한은_비어있다() {
        var props = new CommonSecurityProperties();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter(props, auditProvider());
        Jwt jwt = jwtWithClaim("other", List.of("x"));

        assertThat(roleAuthorities(converter, jwt)).isEmpty();
    }

    @Test
    void rolesClaim과_authorityPrefix는_설정으로_바꿀_수_있다() {
        var props = new CommonSecurityProperties();
        props.setRolesClaim("authorities");
        props.setAuthorityPrefix("SCOPE_");
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter(props, auditProvider());
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

    /** 단위 테스트용 ObjectProvider — 항상 새 SecurityAuditLogger 를 제공. */
    private static ObjectProvider<SecurityAuditLogger> auditProvider() {
        return new ObjectProvider<>() {
            public SecurityAuditLogger getObject() { return new SecurityAuditLogger(); }
            public SecurityAuditLogger getObject(Object... args) { return getObject(); }
            public SecurityAuditLogger getIfAvailable() { return getObject(); }
            public SecurityAuditLogger getIfUnique() { return getObject(); }
        };
    }

    private Jwt jwtWithClaim(String name, Object value) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(name, value)
                .build();
    }
}
