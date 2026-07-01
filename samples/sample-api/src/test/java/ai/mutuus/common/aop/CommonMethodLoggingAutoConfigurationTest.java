package ai.mutuus.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 컨트롤러 메서드 인자/리턴 로깅(AOP) 자동구성의 조건부 동작 검증.
 * <p>기본 OFF(opt-in)이며 {@code enabled=true} + AspectJ classpath 존재일 때만 Aspect/Writer 를 등록한다.
 * Writer 는 {@code @ConditionalOnMissingBean} 이라 소비 서비스가 교체할 수 있다.
 * <p>자동구성 클래스 자체가 {@code AutoConfiguration.imports} 에 등록됐는지의 e2e 활성화 가드는
 * {@code ai.mutuus.sample.LoggingCaseMatrixIntegrationTest}(method.enter/exit 단언)가 담당한다.
 */
class CommonMethodLoggingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonMethodLoggingAutoConfiguration.class));

    @Test
    void 기본은_비활성이라_Aspect를_등록하지_않는다() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ControllerMethodLoggingAspect.class);
            assertThat(ctx).doesNotHaveBean(MethodLoggingWriter.class);
        });
    }

    @Test
    void enabled가_true면_Aspect와_기본Writer가_등록된다() {
        runner.withPropertyValues("mutuus.common.method-logging.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ControllerMethodLoggingAspect.class);
                    assertThat(ctx).hasSingleBean(MethodLoggingWriter.class);
                    assertThat(ctx.getBean(MethodLoggingWriter.class))
                            .isInstanceOf(DefaultMethodLoggingWriter.class);
                });
    }

    @Test
    void AspectJ가_classpath에_없으면_enabled여도_비활성이다() {
        runner.withPropertyValues("mutuus.common.method-logging.enabled=true")
                .withClassLoader(new FilteredClassLoader(ProceedingJoinPoint.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ControllerMethodLoggingAspect.class));
    }

    @Test
    void 소비_서비스가_자체_Writer를_정의하면_그것을_사용한다() {
        MethodLoggingWriter custom = new MethodLoggingWriter() {
            public void onEnter(String type, String method, Object[] args) {
            }

            public void onExit(String type, String method, Object result) {
            }
        };
        runner.withPropertyValues("mutuus.common.method-logging.enabled=true")
                .withBean("customMethodLoggingWriter", MethodLoggingWriter.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(MethodLoggingWriter.class)).isSameAs(custom));
    }
}
