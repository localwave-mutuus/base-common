package ai.mutuus.common.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 영속성 설정 프로퍼티. {@code mutuus.common.persistence.*} 네임스페이스.
 */
@ConfigurationProperties(prefix = "mutuus.common.persistence")
public class CommonPersistenceProperties {

    /** JPA Auditing(감사 컬럼 자동 기록) 활성화 여부. */
    private boolean auditingEnabled = true;

    public boolean isAuditingEnabled() {
        return auditingEnabled;
    }

    public void setAuditingEnabled(boolean auditingEnabled) {
        this.auditingEnabled = auditingEnabled;
    }
}
