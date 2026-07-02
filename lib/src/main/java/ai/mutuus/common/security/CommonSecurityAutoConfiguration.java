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
                                                         JwtAuthenticationConverter jwtAuthenticationConverter,
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
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
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

    /** 시작 시 보안 설정 위험 조합을 점검해 경고 로그를 남긴다(동작 불변). */
    @Bean
    @ConditionalOnMissingBean
    public SecurityConfigAuditor securityConfigAuditor(ObjectProvider<SecurityAuditLogger> securityAuditLogger,
                                                       CommonSecurityProperties props) {
        return new SecurityConfigAuditor(securityAuditLogger.getIfAvailable(SecurityAuditLogger::new), props);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(CommonSecurityProperties props,
            ObjectProvider<SecurityAuditLogger> securityAuditLogger) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                rolesConverter(props, securityAuditLogger.getIfAvailable(SecurityAuditLogger::new)));
        return converter;
    }

    /**
     * roles 클레임 → 권한 변환. {@code roles-claim} 은 <b>중첩 경로</b>(점 구분, 예 {@code realm_access.roles})를
     * 지원한다. 인증됐으나 부여 권한이 0건이면 {@code security.authz.no_authorities} 로 남긴다(fail-closed 진단).
     */
    private Converter<Jwt, Collection<GrantedAuthority>> rolesConverter(CommonSecurityProperties props,
                                                                        SecurityAuditLogger audit) {
        return jwt -> {
            Object claim = resolveClaim(jwt, props.getRolesClaim());
            if (claim instanceof Collection<?> roles && !roles.isEmpty()) {
                return roles.stream()
                        .map(String::valueOf)
                        .map(r -> new SimpleGrantedAuthority(props.getAuthorityPrefix() + r))
                        .map(GrantedAuthority.class::cast)
                        .toList();
            }
            audit.authzNoAuthorities(jwt.getSubject(), props.getRolesClaim());
            return List.of();
        };
    }

    /** 점 구분 경로로 중첩 클레임을 탐색한다(예 {@code realm_access.roles}). 최상위 이름이면 그대로 조회. */
    @SuppressWarnings("unchecked")
    private static Object resolveClaim(Jwt jwt, String path) {
        if (path == null || !path.contains(".")) {
            return jwt.getClaim(path);
        }
        Object current = jwt.getClaims();
        for (String part : path.split("\\.")) {
            if (current instanceof java.util.Map<?, ?> map) {
                current = ((java.util.Map<String, Object>) map).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
