package ai.mutuus.common.config;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.*;
import static org.assertj.core.api.Assertions.*;

class LogFileIdentityTest {
    @Test void 소유자_속성으로_세환경_파일명과_디렉터리를_확정한다() {
        for (String name : new String[]{"local", "dev", "prod"}) {
            var env = environment(Map.of("mutuus.common.service-short-name", "member",
                    "mutuus.common.logging.environment", name, "mutuus.common.logging.serial", "1234abcd",
                    "mutuus.common.logging.directory", "target/logs"));
            new CommonEnvironmentPostProcessor().postProcessEnvironment(env, null);
            assertThat(System.getProperty("mutuus.logFileBase")).isEqualTo(name + "-member-1234abcd-" + ProcessHandle.current().pid());
            assertThat(System.getProperty("mutuus.logDirectory")).isEqualTo("target/logs");
        }
    }
    @Test void local_미지정만_자동일련번호를_생성하고_운영미지정_잘못된값은_거부한다() {
        new CommonEnvironmentPostProcessor().postProcessEnvironment(environment(Map.of()), null);
        assertThat(System.getProperty("mutuus.logFileBase")).matches("local-app-[0-9a-z]{8}-[0-9]+");
        for (var values : java.util.List.of(
                Map.of("mutuus.common.logging.environment", "dev"),
                Map.of("mutuus.common.service-short-name", "MEM"),
                Map.of("mutuus.common.logging.serial", "unsafe/path"))) {
            assertThatThrownBy(() -> new CommonEnvironmentPostProcessor().postProcessEnvironment(environment(values), null)).isInstanceOf(IllegalStateException.class);
        }
    }
    private StandardEnvironment environment(Map<String, String> values) {
        var env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new MapPropertySource("input", new java.util.HashMap<>(values)));
        return env;
    }
}
