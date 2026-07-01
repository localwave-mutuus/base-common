package ai.mutuus.common.core;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link SensitiveDataMasker} — 카드/주민번호 패턴 마스킹(마지막 4자 유지) 검증. */
class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker(List.of(
            Pattern.compile("\\d{13,16}"),
            Pattern.compile("\\d{6}[-]?\\d{7}")));

    @Test
    void 카드번호는_마지막4자만_남기고_마스킹된다() {
        assertThat(masker.mask("card=9876543210123456"))
                .contains("3456")
                .doesNotContain("9876543210123456");
    }

    @Test
    void 주민등록번호는_마스킹된다() {
        assertThat(masker.mask("rrn=990101-1234567"))
                .contains("4567")
                .doesNotContain("1234567");
    }

    @Test
    void 매칭이_없으면_원문_그대로() {
        assertThat(masker.mask("name=hong")).isEqualTo("name=hong");
    }

    @Test
    void null과_빈문자열은_그대로() {
        assertThat(masker.mask(null)).isNull();
        assertThat(masker.mask("")).isEmpty();
    }
}
