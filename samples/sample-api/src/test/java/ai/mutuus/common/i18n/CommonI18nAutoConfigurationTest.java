package ai.mutuus.common.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 다국어 자동구성 검증.
 * <p>컨텍스트에 MessageSource 가 <b>항상 하나만</b> 존재하도록 보장하는 게 핵심이다
 * (표준 MessageSource 가 있으면 폴백을 만들지 않음 — by-type 주입 모호성 방지).
 */
class CommonI18nAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonI18nAutoConfiguration.class));

    @Test
    void 표준_MessageSource가_없으면_공통번들_폴백과_MessageResolver를_제공한다() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(MessageSource.class);
            assertThat(ctx).hasSingleBean(MessageResolver.class);
        });
    }

    @Test
    void Boot의_표준_MessageSource가_있으면_폴백을_만들지_않고_그것을_사용한다() {
        runner.withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(MessageSource.class);
                    assertThat(ctx).hasSingleBean(MessageResolver.class);
                });
    }

    @Test
    void 소비_서비스가_자체_MessageSource를_정의하면_폴백을_만들지_않는다() {
        StaticMessageSource custom = new StaticMessageSource();
        // 컨텍스트 기본 메시지소스 빈 이름과 동일하게 등록해야 폴백 미생성 + 단일 빈이 보장된다.
        runner.withBean("messageSource", MessageSource.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(MessageSource.class);
                    assertThat(ctx.getBean(MessageSource.class)).isSameAs(custom);
                });
    }
}
