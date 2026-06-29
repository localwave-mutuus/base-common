package ai.mutuus.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공통 설정 자동구성 검증.
 * <p>{@code mutuus.common.*} 프로퍼티가 {@link CommonProperties} 로 바인딩되는지 확인한다.
 */
class CommonConfigAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonConfigAutoConfiguration.class));

    @Test
    void CommonProperties가_바인딩되고_기본값을_가진다() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(CommonProperties.class);
            CommonProperties props = ctx.getBean(CommonProperties.class);
            assertThat(props.getServiceName()).isEqualTo("unknown-service");
            assertThat(props.getDefaultLocale()).isEqualTo("ko-KR");
            assertThat(props.isTracingEnabled()).isTrue();
        });
    }

    @Test
    void 소비_서비스의_프로퍼티가_바인딩을_덮어쓴다() {
        runner.withPropertyValues(
                        "mutuus.common.service-name=order-api",
                        "mutuus.common.tracing-enabled=false")
                .run(ctx -> {
                    CommonProperties props = ctx.getBean(CommonProperties.class);
                    assertThat(props.getServiceName()).isEqualTo("order-api");
                    assertThat(props.isTracingEnabled()).isFalse();
                });
    }
}
