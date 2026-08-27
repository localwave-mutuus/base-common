package ai.mutuus.common.secret;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;

/** Golmok의 외부 DB 계약을 읽어 sample 소비자가 암호화 비밀번호로 실제 인증하는 opt-in live 검증. */
@EnabledIf("golmokLiveTestEnabled")
class EncryptedSecretGolmokPostgresLiveIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void sample_secretLocal_프로파일이_Golmok_DB에_암호화_비밀번호로_SELECT1한다() throws Exception {
        DatabaseCredentials credentials = loadGolmokCredentials();
        SecretTestFixture.KeyMaterial keys = SecretTestFixture.createKeyMaterial(tempDir.resolve("keys"));
        String token;
        try {
            token = SecretTestFixture.token(keys.publicKey(), credentials.password(),
                    "sample-api", "local", "database.password", SecretTestFixture.KEY_ID);
        } finally {
            Arrays.fill(credentials.password(), '\0');
        }

        SpringApplication application = new SpringApplication(MinimalDataSourceConsumer.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        try (ConfigurableApplicationContext context = application.run(
                "--spring.profiles.active=secret-local",
                "--GOLMOK_DB_URL=" + credentials.url(),
                "--GOLMOK_DB_USER=" + credentials.username(),
                "--GOLMOK_DB_PASSWORD_TOKEN=" + token,
                "--SAMPLE_SECRET_KEYSTORE=" + keys.keyStore().toAbsolutePath(),
                "--SAMPLE_SECRET_KEYSTORE_PASSWORD_FILE=" + keys.passwordFile().toAbsolutePath(),
                "--SAMPLE_SECRET_ALLOWED_KEY_IDS=" + SecretTestFixture.KEY_ID,
                "--logging.level.root=WARN")) {
            DataSource dataSource = context.getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.prepareStatement("SELECT 1");
                 var result = statement.executeQuery()) {
                assertThat(connection.isValid(2)).isTrue();
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }

    static boolean golmokLiveTestEnabled() {
        return Boolean.getBoolean("live.golmok.secret.enabled")
                && Files.isRegularFile(golmokConfigPath());
    }

    private static DatabaseCredentials loadGolmokCredentials() throws Exception {
        FileSystemResource resource = new FileSystemResource(golmokConfigPath());
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("golmok-local", resource);
        return new DatabaseCredentials(
                configured("live.golmok.url", "GOLMOK_DB_URL", sources, "spring.datasource.url"),
                configured("live.golmok.username", "GOLMOK_DB_USER", sources, "spring.datasource.username"),
                required(sources, "spring.datasource.password").toCharArray());
    }

    private static String configured(String systemProperty, String environmentVariable,
                                     List<PropertySource<?>> sources, String property) {
        String value = System.getProperty(systemProperty);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(environmentVariable);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return required(sources, property);
    }

    private static String required(List<PropertySource<?>> sources, String property) {
        for (PropertySource<?> source : sources) {
            Object value = source.getProperty(property);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        throw new IllegalStateException("Golmok external config is missing required property: " + property);
    }

    private static Path golmokConfigPath() {
        return Path.of(System.getProperty("live.golmok.config",
                Path.of(System.getProperty("user.home"), ".mutuus", "golmok-backend", "local.yml").toString()));
    }

    private record DatabaseCredentials(String url, String username, char[] password) {
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
    static class MinimalDataSourceConsumer {
    }
}
