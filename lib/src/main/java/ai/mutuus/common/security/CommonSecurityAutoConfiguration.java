package ai.mutuus.common.security;

import java.util.Collection;
import java.util.List;

import ai.mutuus.common.logging.AccessLogger;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
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
                                                         ObjectProvider<AccessLogger> accessLogger,
                                                         ObjectProvider<SecurityAuditLogger> securityAuditLogger)
            throws Exception {
        // 액세스/보안감사 로깅이 비활성(빈 없음)이면 무동작 로거로 폴백
        AccessLogger logger = accessLogger.getIfAvailable(AccessLogger::new);
        SecurityAuditLogger audit = securityAuditLogger.getIfAvailable(SecurityAuditLogger::new);
        var entryPoint = new LoggingAuthenticationEntryPoint(new BearerTokenAuthenticationEntryPoint(), logger, audit);
        var deniedHandler = new LoggingAccessDeniedHandler(new BearerTokenAccessDeniedHandler(), logger, audit);

        http
                // 무상태 OAuth2 리소스 서버: 세션을 만들지 않고, 토큰 기반이라 CSRF 보호 불필요.
                // (이 설정이 없으면 permit-all 경로라도 POST 등 변경 요청이 CSRF로 403 처리됨)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(props.getPermitAll().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                // 최상위 예외 처리: 필터/메서드 보안(@PreAuthorize)에서 발생한 인증실패·인가거부를 모두
                // 로깅 핸들러로 라우팅한다(이게 없으면 메서드 보안 403 은 기본 핸들러로 빠져 보안 로그가 남지 않음).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(props))))
                // 인증 확정 후 실제 주체를 추적/로깅 컨텍스트에 반영 + 인입 X-User-Id 위장 탐지
                .addFilterAfter(new AuthenticatedUserContextFilter(audit, props.isTrustForwardedUser()),
                        AuthorizationFilter.class);
        return http.build();
    }

    /**
     * 메서드 보안(@PreAuthorize) 등 MVC 내부에서 발생한 인가 거부를 403 + 보안 로그로 바로잡는 어드바이스.
     * (필터 단계 인가 거부는 {@link LoggingAccessDeniedHandler} 가 처리)
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityExceptionAdvice securityExceptionAdvice(
            ai.mutuus.common.i18n.MessageResolver messages,
            ObjectProvider<SecurityAuditLogger> securityAuditLogger) {
        return new SecurityExceptionAdvice(messages, securityAuditLogger.getIfAvailable(SecurityAuditLogger::new));
    }

    /**
     * JWT 검증기(만료 + issuer/audience 하드닝)를 적용한 자원 서버 {@link JwtDecoder}. jwk-set-uri 가 있고
     * 소비 서비스가 자체 디코더를 정의하지 않은 경우에만 등록한다(그 경우 Boot 기본 디코더를 대체). audiences/issuer 를
     * {@code mutuus.common.security.*} 로 지정하면 <b>다른 서비스용 토큰(aud 불일치)</b>이 거부된다.
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "jwk-set-uri")
    public JwtDecoder commonJwtDecoder(CommonSecurityProperties props,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(CommonJwtValidators.build(props.getAudiences(), props.getIssuer()));
        return decoder;
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
