package ai.mutuus.common.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 액세스 로깅 자동구성의 조건부 동작 검증.
 * <p>{@link AccessLogger} 는 항상 제공(보안/예외 모듈이 의존)하되, 요청/응답 필터는
 * 웹 환경에서만 등록되고, {@code enabled=false} 면 전체가 비활성화된다.
 */
class CommonLoggingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonLoggingAutoConfiguration.class));

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonLoggingAutoConfiguration.class));

    @Test
    void 비웹_환경에서는_AccessLogger만_제공하고_요청필터는_등록하지_않는다() {
        runner.withClassLoader(new FilteredClassLoader(DispatcherServlet.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(AccessLogger.class);
                    assertThat(ctx).doesNotHaveBean("accessLogFilterRegistration");
                });
    }

    @Test
    void 웹_환경에서는_AccessLogger와_요청응답필터가_함께_등록된다() {
        webRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(AccessLogger.class);
            assertThat(ctx).hasBean("accessLogFilterRegistration");
        });
    }

    @Test
    void enabled가_false면_로깅_자동구성_전체가_비활성화된다() {
        webRunner.withPropertyValues("mutuus.common.logging.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(AccessLogger.class);
                    assertThat(ctx).doesNotHaveBean("accessLogFilterRegistration");
                });
    }

    @Test
    void 소비_서비스가_자체_AccessLogger를_정의하면_그것을_사용한다() {
        AccessLogger custom = new AccessLogger();
        runner.withBean("customAccessLogger", AccessLogger.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(AccessLogger.class)).isSameAs(custom));
    }
}
