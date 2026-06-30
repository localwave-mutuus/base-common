package ai.mutuus.common.persistence;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 공통 영속성 자동 구성. JPA(spring-data-jpa)가 classpath 에 있을 때만 동작한다.
 * <p>감사 주체 제공자({@link TraceContextAuditorAware})를 등록한다. JPA Auditing 활성화는
 * 실제 JPA 부트스트랩이 필요하므로 {@link JpaAuditingAutoConfiguration} 에서 별도로 켠다.
 * <p>{@code mutuus.common.persistence.auditing-enabled=false} 로 끌 수 있고, 소비 서비스가
 * 자체 {@code AuditorAware} 를 정의했다면 그 값이 우선한다.
 */
@AutoConfiguration
@ConditionalOnClass(AuditingEntityListener.class)
@ConditionalOnProperty(prefix = "mutuus.common.persistence", name = "auditing-enabled", matchIfMissing = true)
@EnableConfigurationProperties(CommonPersistenceProperties.class)
public class CommonPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> traceContextAuditorAware() {
        return new TraceContextAuditorAware();
    }
}
