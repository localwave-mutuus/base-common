package ai.mutuus.common.config;

import java.util.Map;
import java.util.HashMap;
import ai.mutuus.common.core.LogFilePolicy;
import ai.mutuus.common.logging.CommonLoggingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.boot.context.properties.bind.Binder;
import static org.assertj.core.api.Assertions.*;

class LogFilePolicyConfigurationTest {
    private final Map<String, String> old = new HashMap<>();
    private final String[] keys = {"mutuus.appCode", "mutuus.instanceCode", "mutuus.logFileBase",
            "mutuus.logDirectory", "SERVICE_NAME", "mutuus.environment", "mutuus.dataStreamNamespace",
            "mutuus.logRollInterval", "mutuus.logMaxFileSize"};
    @BeforeEach void save() { for (String key : keys) old.put(key, System.getProperty(key)); }
    @AfterEach void restore() {
        old.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
    }
    private StandardEnvironment apply(Map<String, Object> input) {
        var env = new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new MapPropertySource("project", input));
        new CommonEnvironmentPostProcessor().postProcessEnvironment(env, null);
        return env;
    }
    @Test void 기본값은_한시간_100MB() {
        var env = apply(Map.of());
        assertThat(env.getProperty("mutuus.common.logging.file.roll-interval")).isEqualTo("PT1H");
        assertThat(env.getProperty("mutuus.common.logging.file.max-file-size")).isEqualTo("104857600B");
    }
    @Test void 프로젝트_YAML값을_Logback과_프로퍼티빈이_같이_사용한다() {
        var env = apply(Map.of("mutuus.common.logging.file.roll-interval", "PT5M",
                "mutuus.common.logging.file.max-file-size", "2MB"));
        var props = Binder.get(env).bind("mutuus.common.logging", CommonLoggingProperties.class).get();
        assertThat(props.getFile().getRollInterval()).isEqualTo("PT5M");
        assertThat(props.getFile().getMaxFileSize()).isEqualTo("2097152B");
        assertThat(System.getProperty("mutuus.logRollInterval")).isEqualTo("PT5M");
        assertThat(System.getProperty("mutuus.logMaxFileSize")).isEqualTo("2097152");
    }
    @Test void 명시한_GS환경변수가_YAML보다_우선한다() {
        var env = apply(Map.of("mutuus.common.logging.file.roll-interval", "PT1H",
                "mutuus.common.logging.file.max-file-size", "100MB",
                "GS_LOG_ROLL_INTERVAL", "PT5M", "GS_LOG_MAX_FILE_SIZE", "512KB"));
        assertThat(env.getProperty("mutuus.common.logging.file.roll-interval")).isEqualTo("PT5M");
        assertThat(env.getProperty("mutuus.common.logging.file.max-file-size")).isEqualTo("524288B");
    }
    @Test void 이전_키는_canonical값이_없을때만_적용한다() {
        assertThat(apply(Map.of("mutuus.log.roll-interval", "PT30M"))
                .getProperty("mutuus.common.logging.file.roll-interval")).isEqualTo("PT30M");
        assertThat(apply(Map.of("mutuus.log.roll-interval", "PT30M", "mutuus.common.logging.file.roll-interval", "PT5M"))
                .getProperty("mutuus.common.logging.file.roll-interval")).isEqualTo("PT5M");
    }
    @Test void 잘못된_시간은_설정이름을_표시하며_기동전_실패한다() {
        for (String value : new String[]{"", "5m", "PT0S", "PT59S", "PT7M", "PT2H", "PT24H", "PT25H", "PT-1M"}) {
            assertThatThrownBy(() -> apply(Map.of("mutuus.common.logging.file.roll-interval", value)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("file.roll-interval");
        }
    }
    @Test void 잘못된_크기는_설정이름을_표시하며_기동전_실패한다() {
        for (String value : new String[]{"", "0", "-1MB", "1.5MB", "100XB", "9223372036854775807TB"}) {
            assertThatThrownBy(() -> apply(Map.of("GS_LOG_MAX_FILE_SIZE", value)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("file.max-file-size");
        }
    }
    @Test void 초단위_약수와_양의_바이트_경계를_지원한다() {
        assertThat(LogFilePolicy.rollInterval("PT90S")).isEqualTo(java.time.Duration.ofSeconds(90));
        assertThat(LogFilePolicy.maxFileSize("1B")).isEqualTo(1);
        assertThat(LogFilePolicy.maxFileSize("1gb")).isEqualTo(1073741824);
        assertThat(LogFilePolicy.maxFileSize("9223372036854775807B")).isEqualTo(Long.MAX_VALUE);
    }
}
