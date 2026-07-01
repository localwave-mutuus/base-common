package ai.mutuus.common.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link PageResponse#of} 의 페이지 메타 계산 검증(순수 단위). */
class PageResponseTest {

    @Test
    void of_는_페이지_메타를_계산한다() {
        PageResponse<String> p = PageResponse.of(List.of("a", "b"), 0, 2, 5);
        assertThat(p.totalPages()).isEqualTo(3);
        assertThat(p.first()).isTrue();
        assertThat(p.last()).isFalse();
        assertThat(p.numberOfElements()).isEqualTo(2);
    }

    @Test
    void 마지막_페이지는_last가_true() {
        PageResponse<String> p = PageResponse.of(List.of("e"), 2, 2, 5);
        assertThat(p.first()).isFalse();
        assertThat(p.last()).isTrue();
        assertThat(p.numberOfElements()).isEqualTo(1);
    }

    @Test
    void 빈_결과는_totalPages0_이고_first와_last가_true() {
        PageResponse<String> p = PageResponse.of(List.of(), 0, 10, 0);
        assertThat(p.totalPages()).isZero();
        assertThat(p.first()).isTrue();
        assertThat(p.last()).isTrue();
        assertThat(p.numberOfElements()).isZero();
    }
}
