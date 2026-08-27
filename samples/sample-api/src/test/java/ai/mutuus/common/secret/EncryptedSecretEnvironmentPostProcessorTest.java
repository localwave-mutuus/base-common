package ai.mutuus.common.secret;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedSecretEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    private SecretTestFixture.KeyMaterial keys;

    @BeforeEach
    void setUp() throws Exception {
        keys = SecretTestFixture.createKeyMaterial(tempDir);
    }

    @Test
    void optIn이면_모든_검증_후_최우선_propertySource에_복호화값을_원자등록한다() throws Exception {
        String token = token("fixture-db-password");
        MockEnvironment environment = environment(token);

        processor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().get(
                EncryptedSecretEnvironmentPostProcessor.DECRYPTED_PROPERTY_SOURCE)).isNotNull();
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("fixture-db-password");
    }

    @Test
    void optOut이면_평문을_변경하거나_복호화_propertySource를_추가하지_않는다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.password", "legacy-external-password");

        processor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("legacy-external-password");
        assertThat(environment.getPropertySources().contains(
                EncryptedSecretEnvironmentPostProcessor.DECRYPTED_PROPERTY_SOURCE)).isFalse();
    }

    @Test
    void 암호문과_평문이_서로_다른_source에_있으면_failClosed한다() throws Exception {
        MockEnvironment environment = environment(token("fixture-db-password"));
        environment.getPropertySources().addFirst(new MapPropertySource(
                "systemEnvironment", Map.of("spring.datasource.password", "competing-plaintext")));

        assertThatThrownBy(() -> processor().postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOfSatisfying(SecretResolutionException.class, exception ->
                        assertThat(exception.reason())
                                .isEqualTo(SecretResolutionException.Reason.PROPERTY_SOURCE_CONFLICT));
        assertThat(environment.getPropertySources().contains(
                EncryptedSecretEnvironmentPostProcessor.DECRYPTED_PROPERTY_SOURCE)).isFalse();
    }

    @Test
    void malformed_토큰이면_평문이나_부분결과로_fallback하지_않는다() {
        MockEnvironment environment = environment("secret:v2:inline:not_valid=");

        assertThatThrownBy(() -> processor().postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOfSatisfying(SecretResolutionException.class, exception -> {
                    assertThat(exception.reason()).isEqualTo(SecretResolutionException.Reason.MALFORMED_TOKEN);
                    assertThat(exception.getMessage())
                            .doesNotContain("not_valid")
                            .doesNotContain("test-only-keystore-password");
                });
        assertThat(environment.getPropertySources().contains(
                EncryptedSecretEnvironmentPostProcessor.DECRYPTED_PROPERTY_SOURCE)).isFalse();
    }

    private EncryptedSecretEnvironmentPostProcessor processor() {
        return new EncryptedSecretEnvironmentPostProcessor(
                new DefaultEncryptedSecretResolver(),
                LogFactory.getLog(EncryptedSecretEnvironmentPostProcessor.class));
    }

    private MockEnvironment environment(String token) {
        MockEnvironment environment = new MockEnvironment();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("spring.application.name", "sample-api");
        values.put("mutuus.common.secret.enabled", "true");
        values.put("mutuus.common.secret.environment", "local");
        values.put("mutuus.common.secret.keystore",
                "file:" + keys.keyStore().toAbsolutePath().toString().replace('\\', '/'));
        values.put("mutuus.common.secret.keystore-password-file", keys.passwordFile().toString());
        values.put("mutuus.common.secret.allowed-key-ids", SecretTestFixture.KEY_ID);
        values.put("spring.datasource.password", token);
        environment.getPropertySources().addFirst(new MapPropertySource("applicationConfig", values));
        return environment;
    }

    private String token(String plaintext) throws Exception {
        char[] value = plaintext.toCharArray();
        try {
            return SecretTestFixture.token(keys.publicKey(), value,
                    "sample-api", "local", "database.password", SecretTestFixture.KEY_ID);
        } finally {
            java.util.Arrays.fill(value, '\0');
        }
    }
}
