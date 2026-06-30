package ai.mutuus.common.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 감사 컬럼(생성/수정 시각·주체)을 제공하는 공통 베이스 엔티티.
 * <p>JPA Auditing 으로 자동 채워진다. 생성/수정 <b>주체</b>는 {@link TraceContextAuditorAware}
 * 가 {@code TraceContext} 의 인증 사용자({@code X-User-Id})에서 가져오므로, 도메인 코드가
 * 직접 채우지 않아도 신뢰 가능한 사용자 식별자가 기록된다.
 * <p>식별자(@Id) 전략은 도메인마다 달라 의도적으로 강제하지 않는다 — 엔티티가 직접 정의한다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 64)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
