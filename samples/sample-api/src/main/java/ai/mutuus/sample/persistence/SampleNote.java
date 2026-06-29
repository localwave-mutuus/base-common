package ai.mutuus.sample.persistence;

import ai.mutuus.common.persistence.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * 라이브러리의 {@link BaseEntity} 를 상속해 감사 컬럼(생성/수정 시각·주체)을 자동으로
 * 부여받는 데모 엔티티. 도메인은 식별자와 자기 필드만 정의한다.
 */
@Entity
public class SampleNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    protected SampleNote() {
    }

    public SampleNote(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
