package ai.mutuus.common.secret;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedSecretBootstrapIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void springFactories_processor가_ConfigData_이후_컨텍스트_이전에_복호화한다() throws Exception {
        SecretTestFixture.KeyMaterial keys = SecretTestFixture.createKeyMaterial(tempDir.resolve("keys"));
        char[] plaintext = "bootstrap-fixture-password".toCharArray();
        String token;
        try {
            token = SecretTestFixture.token(keys.publicKey(), plaintext,
                    "bootstrap-consumer", "test", "database.password", SecretTestFixture.KEY_ID);
        } finally {
            java.util.Arrays.fill(plaintext, '\0');
        }

        Path config = tempDir.resolve("config");
        Files.createDirectories(config);
        Files.writeString(config.resolve("encrypted-test.properties"), """
                spring.application.name=bootstrap-consumer
                mutuus.common.secret.enabled=true
                mutuus.common.secret.environment=test
                mutuus.common.secret.keystore=%s
                mutuus.common.secret.keystore-password-file=%s
                mutuus.common.secret.allowed-key-ids=%s
                spring.datasource.password=%s
                """.formatted(
                propertyPath(keys.keyStore()),
                propertyPath(keys.passwordFile()),
                SecretTestFixture.KEY_ID,
                token), StandardCharsets.UTF_8);

        SpringApplication application = new SpringApplication(MinimalApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        try (ConfigurableApplicationContext context = application.run(
                "--spring.config.name=encrypted-test",
                "--spring.config.location=" + config.toUri())) {
            ConfigurableEnvironment environment = context.getEnvironment();
            assertThat(environment.getPropertySources().contains(
                    EncryptedSecretEnvironmentPostProcessor.DECRYPTED_PROPERTY_SOURCE)).isTrue();
            assertThat(environment.getProperty("spring.datasource.password"))
                    .isEqualTo("bootstrap-fixture-password");
        }
    }

    private static String propertyPath(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }

    @Configuration(proxyBeanMethods = false)
    static class MinimalApplication {
    }
}
