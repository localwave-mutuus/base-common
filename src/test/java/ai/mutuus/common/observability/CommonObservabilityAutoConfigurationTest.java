package ai.mutuus.common.observability;

import ai.mutuus.common.observability.CommonObservabilityAutoConfiguration.CommonObservationMarker;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관측 자동구성의 조건부 동작 검증.
 * <p>마커 빈은 Micrometer 가 classpath 에 있고({@code @ConditionalOnClass}),
 * 실제 {@link ObservationRegistry} 빈이 존재할 때만({@code @ConditionalOnBean}) 등록된다.
 */
class CommonObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonObservabilityAutoConfiguration.class));

    @Test
    void ObservationRegistry_빈이_있으면_마커가_등록된다() {
        runner.withBean(ObservationRegistry.class, ObservationRegistry::create)
                .run(ctx -> assertThat(ctx).hasSingleBean(CommonObservationMarker.class));
    }

    @Test
    void ObservationRegistry_빈이_없으면_마커가_등록되지_않는다() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(CommonObservationMarker.class));
    }

    @Test
    void Micrometer가_classpath에_없으면_자동구성_전체가_비활성화된다() {
        runner.withClassLoader(new FilteredClassLoader(ObservationRegistry.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(CommonObservationMarker.class));
    }
}
