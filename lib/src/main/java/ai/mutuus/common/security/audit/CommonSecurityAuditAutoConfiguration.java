package ai.mutuus.common.security.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 보안 감사 로깅 자동 구성. {@link SecurityAuditLogger} 를 항상 제공(빈 없으면 폴백)하여 보안/웹 모듈이
 * 위배 이벤트를 남길 수 있게 한다. 클래스 조건 없이 프로퍼티 토글({@code mutuus.common.security.audit.enabled},
 * 기본 ON)로만 제어하므로, 보안 스타터가 없는 비웹 서비스도(전파/설정점검 등) 필요 시 주입해 쓸 수 있다.
 * 소비 서비스가 자체 {@link SecurityAuditLogger} 빈을 정의하면 그 값이 우선한다.
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonSecurityAuditProperties.class)
@ConditionalOnProperty(prefix = "mutuus.common.security.audit", name = "enabled", matchIfMissing = true)
public class CommonSecurityAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityAuditLogger securityAuditLogger() {
        return new SecurityAuditLogger();
    }
}
