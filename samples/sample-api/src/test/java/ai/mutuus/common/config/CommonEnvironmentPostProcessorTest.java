package ai.mutuus.common.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonEnvironmentPostProcessorTest {

    private final CommonEnvironmentPostProcessor processor = new CommonEnvironmentPostProcessor();

    @Test
    void 강제_굴림은_기본_300초이며_env가_YAML보다_우선하고_0으로_끈다() {
        String internal = "mutuus.logForceRollSeconds";
        String previous = System.getProperty(internal);
        try {
            var defaults = new StandardEnvironment();
            processor.postProcessEnvironment(defaults, null);
            assertThat(defaults.getProperty("mutuus.common.logging.file.force-roll-seconds")).isEqualTo("300");
            assertThat(System.getProperty(internal)).isEqualTo("300");
            var override = new StandardEnvironment();
            override.getPropertySources().addFirst(new MapPropertySource("applicationConfig", Map.of(
                    "mutuus.common.logging.file.force-roll-seconds", "120", "GS_LOG_FORCE_ROLL_SECONDS", "0")));
            processor.postProcessEnvironment(override, null);
            assertThat(override.getProperty("mutuus.common.logging.file.force-roll-seconds")).isEqualTo("0");
            assertThat(System.getProperty(internal)).isEqualTo("0");
            var invalid = new StandardEnvironment();
            invalid.getPropertySources().addFirst(new MapPropertySource("invalid", Map.of("GS_LOG_FORCE_ROLL_SECONDS", "-1")));
            assertThatThrownBy(() -> processor.postProcessEnvironment(invalid, null)).isInstanceOf(IllegalArgumentException.class);
        } finally {
            if (previous == null) System.clearProperty(internal); else System.setProperty(internal, previous);
        }
    }

    @Test
    void 비웹_JavaHttp도_애플리케이션_W3C_비활성_설정을_받는다() {
        String key = ai.mutuus.common.core.TraceHeaders.W3C_RUNTIME_PROPERTY;
        String previous = System.getProperty(key);
        try {
            var env = new StandardEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("applicationConfig",
                    Map.of("mutuus.common.tracing.w3c", "false")));
            processor.postProcessEnvironment(env, null);
            assertThat(System.getProperty(key)).isEqualTo("false");
            assertThat(env.getProperty("mutuus.common.tracing.w3c")).isEqualTo("false");
        } finally {
            if (previous == null) System.clearProperty(key); else System.setProperty(key, previous);
        }
    }

    @Test
    void 컨벤션_기본값을_Environment에_주입한다() {
        var env = new StandardEnvironment();

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("mutuus.common.default-locale")).isEqualTo("ko-KR");
        assertThat(env.getProperty("mutuus.common.tracing-enabled")).isEqualTo("true");
        assertThat(env.getProperty("spring.messages.basename")).isEqualTo("messages/messages");
        // HTTP 클라이언트 기본 타임아웃(무한 대기 방지) 컨벤션
        assertThat(env.getProperty("spring.http.client.connect-timeout")).isEqualTo("2s");
        assertThat(env.getProperty("spring.http.client.read-timeout")).isEqualTo("10s");
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
