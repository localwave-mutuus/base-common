package ai.mutuus.common.logging;

import ai.mutuus.common.core.SensitiveDataMasker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** 로그 마스킹 자동구성 조건부 동작(기본 OFF / enabled 시 masker 등록 / 소비자 대체). */
class CommonMaskingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonMaskingAutoConfiguration.class));

    @Test
    void 기본은_비활성이라_masker를_등록하지_않는다() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(SensitiveDataMasker.class));
    }

    @Test
    void enabled가_true면_SensitiveDataMasker가_등록된다() {
        runner.withPropertyValues("mutuus.common.logging.masking.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(SensitiveDataMasker.class));
    }

    @Test
    void 소비자가_자체_masker를_정의하면_그것을_사용한다() {
        SensitiveDataMasker custom = new SensitiveDataMasker(java.util.List.of());
        runner.withPropertyValues("mutuus.common.logging.masking.enabled=true")
                .withBean("customMasker", SensitiveDataMasker.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(SensitiveDataMasker.class)).isSameAs(custom));
    }
}
