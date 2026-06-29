package ai.mutuus.common.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CommonEnvironmentPostProcessorTest {

    private final CommonEnvironmentPostProcessor processor = new CommonEnvironmentPostProcessor();

    @Test
    void 컨벤션_기본값을_Environment에_주입한다() {
        var env = new StandardEnvironment();

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("mutuus.common.default-locale")).isEqualTo("ko-KR");
        assertThat(env.getProperty("mutuus.common.tracing-enabled")).isEqualTo("true");
        assertThat(env.getProperty("spring.messages.basename")).isEqualTo("messages/messages");
    }

    @Test
    void 애플리케이션_설정이_기본값보다_우선한다() {
        var env = new StandardEnvironment();
        // 애플리케이션이 정의한 값(높은 우선순위)을 먼저 추가
        env.getPropertySources().addFirst(new MapPropertySource(
                "applicationConfig", Map.of("mutuus.common.default-locale", "en-US")));

        processor.postProcessEnvironment(env, null);   // addLast(최저 우선순위)로 주입되므로 덮어쓰지 못함

        assertThat(env.getProperty("mutuus.common.default-locale")).isEqualTo("en-US");
    }
}
