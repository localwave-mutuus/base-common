package ai.mutuus.common.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void isBlank_hasText() {
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank("  ")).isTrue();
        assertThat(StringUtils.isBlank("x")).isFalse();
        assertThat(StringUtils.hasText("x")).isTrue();
    }

    @Test
    void defaultIfBlank는_공백이면_기본값을_반환한다() {
        assertThat(StringUtils.defaultIfBlank(null, "d")).isEqualTo("d");
        assertThat(StringUtils.defaultIfBlank("  ", "d")).isEqualTo("d");
        assertThat(StringUtils.defaultIfBlank("v", "d")).isEqualTo("v");
    }

    @Test
    void mask는_앞_visible_글자만_남기고_가린다() {
        assertThat(StringUtils.mask("01012345678", 3)).isEqualTo("010********");
        assertThat(StringUtils.mask("ab", 3)).isEqualTo("**");      // 길이 <= visible
        assertThat(StringUtils.mask(null, 3)).isNull();
    }
}
