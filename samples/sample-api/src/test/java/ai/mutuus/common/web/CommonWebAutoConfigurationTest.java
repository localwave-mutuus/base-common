package ai.mutuus.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.LocaleResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * optional 의존성 + 조건부 자동구성 검증.
 * <p>이 라이브러리의 핵심 설계(@ConditionalOnClass/@ConditionalOnProperty)가
 * 의도대로 빈을 켜고 끄는지 확인한다.
 */
class CommonWebAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class));

    @Test
    void 웹_환경이면_추적필터_헤더전파_로케일리졸버가_등록된다() {
        runner.run(ctx -> {
            assertThat(ctx).hasBean("traceFilterRegistration");
            assertThat(ctx).hasSingleBean(HeaderPropagationInterceptor.class);
            assertThat(ctx).hasSingleBean(LocaleResolver.class);
        });
    }

    @Test
    void tracing_enabled가_false면_웹_자동구성이_비활성화된다() {
        runner.withPropertyValues("mutuus.common.tracing-enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(HeaderPropagationInterceptor.class));
    }
}
