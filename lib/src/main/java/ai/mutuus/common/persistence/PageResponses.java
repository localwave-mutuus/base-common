package ai.mutuus.common.persistence;

import ai.mutuus.common.api.PageResponse;
import org.springframework.data.domain.Page;

/**
 * Spring Data {@link Page} → {@link PageResponse} 어댑터(optional 유틸).
 * <p>이 클래스는 {@code org.springframework.data.domain.Page} 를 참조하므로 <b>호출 시점에만</b>
 * 로드된다 — spring-data 를 쓰는 소비자(JPA 등)만 참조하면 되고, 비(非)JPA 소비자는 로드조차 되지 않아
 * {@code PageResponse} 자체의 순수성(spring-data 비의존)을 해치지 않는다.
 */
public final class PageResponses {

    private PageResponses() {
    }

    /** JPA 리포지토리 등이 반환한 {@code Page} 를 표준 {@link PageResponse} 로 변환한다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
