package ai.mutuus.common.persistence;

import java.util.List;

import ai.mutuus.common.api.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** Spring Data {@code Page} → {@link PageResponse} 어댑터 검증(spring-data 존재 시). */
class PageResponsesTest {

    @Test
    void Spring_Data_Page를_PageResponse로_변환한다() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);

        PageResponse<String> r = PageResponses.from(page);

        assertThat(r.content()).containsExactly("a", "b");
        assertThat(r.page()).isZero();
        assertThat(r.size()).isEqualTo(2);
        assertThat(r.totalElements()).isEqualTo(5);
        assertThat(r.totalPages()).isEqualTo(3);
        assertThat(r.first()).isTrue();
        assertThat(r.last()).isFalse();
        assertThat(r.numberOfElements()).isEqualTo(2);
    }
}
