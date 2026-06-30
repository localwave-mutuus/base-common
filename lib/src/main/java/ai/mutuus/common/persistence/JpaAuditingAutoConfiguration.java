package ai.mutuus.common.persistence;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 자동 구성. 실제 JPA 가 부트스트랩된 뒤(EntityManagerFactory 생성 이후)
 * 동작하도록 {@code HibernateJpaAutoConfiguration} 다음으로 정렬한다.
 * <p>{@link CommonPersistenceAutoConfiguration} 가 등록한 단일 {@code AuditorAware} 빈을
 * 자동 감지해, {@link BaseEntity} 상속 엔티티의 생성/수정 시각·주체를 기록한다.
 * 소비 서비스가 이미 JPA Auditing 을 켰다면({@code jpaAuditingHandler} 존재) 중복 활성화하지 않는다.
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration")
@ConditionalOnClass(AuditingEntityListener.class)
@ConditionalOnProperty(prefix = "mutuus.common.persistence", name = "auditing-enabled", matchIfMissing = true)
@ConditionalOnMissingBean(name = "jpaAuditingHandler")
@EnableJpaAuditing
public class JpaAuditingAutoConfiguration {
}
