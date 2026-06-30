package ai.mutuus.common.security;

import java.util.Collection;
import java.util.List;

import ai.mutuus.common.logging.AccessLogger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * 공통 보안 자동 구성.
 * <p>별도 인증/인가 MSA가 발급한 JWT를 검증하는 OAuth2 Resource Server 기본 설정을 제공한다.
 * issuer-uri/jwk-set-uri 는 {@code spring.security.oauth2.resourceserver.jwt.*} 로 주입한다.
 * 애플리케이션이 자체 SecurityFilterChain 빈을 정의하면 이 기본값은 비활성화된다.
 */
@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(CommonSecurityProperties.class)
public class CommonSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain commonSecurityFilterChain(HttpSecurity http,
                                                         CommonSecurityProperties props,
                                                         ObjectProvider<AccessLogger> accessLogger) throws Exception {
        // 액세스 로깅이 비활성(빈 없음)이면 무동작 로거로 폴백
        AccessLogger logger = accessLogger.getIfAvailable(AccessLogger::new);
        var entryPoint = new LoggingAuthenticationEntryPoint(new BearerTokenAuthenticationEntryPoint(), logger);
        var deniedHandler = new LoggingAccessDeniedHandler(new BearerTokenAccessDeniedHandler(), logger);

        http
                // 무상태 OAuth2 리소스 서버: 세션을 만들지 않고, 토큰 기반이라 CSRF 보호 불필요.
                // (이 설정이 없으면 permit-all 경로라도 POST 등 변경 요청이 CSRF로 403 처리됨)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(props.getPermitAll().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(props))))
                // 인증 확정 후 실제 주체를 추적/로깅 컨텍스트에 반영
                .addFilterAfter(new AuthenticatedUserContextFilter(), AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(CommonSecurityProperties props) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesConverter(props));
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> rolesConverter(CommonSecurityProperties props) {
        return jwt -> {
            Object claim = jwt.getClaim(props.getRolesClaim());
            if (claim instanceof Collection<?> roles) {
                return roles.stream()
                        .map(String::valueOf)
                        .map(r -> new SimpleGrantedAuthority(props.getAuthorityPrefix() + r))
                        .map(GrantedAuthority.class::cast)
                        .toList();
            }
            return List.of();
        };
    }
}
