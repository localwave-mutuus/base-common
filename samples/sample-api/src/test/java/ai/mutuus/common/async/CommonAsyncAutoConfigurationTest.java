package ai.mutuus.common.async;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비동기 전파 자동구성의 조건부 동작 검증.
 * <p>{@link TraceContextTaskDecorator} 빈 등록 여부가 추적 스위치/클래스/사용자 빈 조건에
 * 따라 의도대로 켜고 꺼지는지 확인한다.
 */
class CommonAsyncAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonAsyncAutoConfiguration.class));

    @Test
    void 기본적으로_추적전파_TaskDecorator가_등록된다() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(TaskDecorator.class);
            assertThat(ctx.getBean(TaskDecorator.class)).isInstanceOf(TraceContextTaskDecorator.class);
        });
    }

    @Test
    void tracing_enabled가_false면_등록되지_않는다() {
        runner.withPropertyValues("mutuus.common.tracing-enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(TaskDecorator.class));
    }

    @Test
    void TaskDecorator_클래스가_없으면_자동구성_전체가_비활성화된다() {
        runner.withClassLoader(new FilteredClassLoader(TaskDecorator.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean("traceContextTaskDecorator"));
    }

    @Test
    void 소비_서비스가_자체_TaskDecorator를_정의하면_그것을_사용한다() {
        TaskDecorator custom = runnable -> runnable;
        runner.withBean("customDecorator", TaskDecorator.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(TaskDecorator.class);
                    assertThat(ctx.getBean(TaskDecorator.class)).isSameAs(custom);
                });
    }
}
