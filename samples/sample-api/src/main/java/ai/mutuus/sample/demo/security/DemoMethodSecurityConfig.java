package ai.mutuus.sample.demo.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * {@code @PreAuthorize} 등 메서드 보안을 켠다(라이브러리 기본 체인은 경로 기반만 제공). 데모의 관리자 권한
 * 엔드포인트({@link SecurityDemoController#adminPing()})가 {@code ROLE_ADMIN} 을 요구해 403(authz.denied)을
 * 유발할 수 있게 한다. 메서드 보안이 던지는 {@code AccessDeniedException} 은 자원 서버 DSL 에 주입된
 * {@code LoggingAccessDeniedHandler} 로 흘러 보안 감사 로그로 남는다.
 */
@Configuration
@EnableMethodSecurity
public class DemoMethodSecurityConfig {
}
