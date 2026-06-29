package ai.mutuus.common.exception;

import ai.mutuus.common.i18n.MessageResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예외 처리 자동구성의 조건부 동작 검증.
 * <p>{@link GlobalExceptionHandler} 는 웹(MVC) 환경에서만 등록되어야 한다
 * (비웹 배치 서비스가 라이브러리를 써도 MVC 의존이 끌려오지 않도록).
 */
class CommonExceptionAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(MessageResolver.class, () -> new MessageResolver(new StaticMessageSource()))
            .withConfiguration(AutoConfigurations.of(CommonExceptionAutoConfiguration.class));

    @Test
    void 웹_환경이면_GlobalExceptionHandler가_등록된다() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    void DispatcherServlet이_없는_비웹_환경에서는_등록되지_않는다() {
        runner.withClassLoader(new FilteredClassLoader(DispatcherServlet.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Test
    void 소비_서비스가_자체_핸들러를_정의하면_그것을_사용한다() {
        GlobalExceptionHandler custom = new GlobalExceptionHandler(new MessageResolver(new StaticMessageSource()), null);
        runner.withBean("customHandler", GlobalExceptionHandler.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(GlobalExceptionHandler.class)).isSameAs(custom));
    }
}
