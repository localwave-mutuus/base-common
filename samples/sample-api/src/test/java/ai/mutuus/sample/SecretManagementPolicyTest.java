package ai.mutuus.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * File-level guard for the canonical encrypted-property standard and the legacy external-file bridge.
 *
 * <p>This test does not connect to PostgreSQL, Redis, Nexus, or any fixed port.
 */
class SecretManagementPolicyTest {

    @Test
    void local_profile_imports_external_secret_file_and_has_no_plain_sample_password() throws IOException {
        String text = Files.readString(root("samples/sample-api/src/main/resources/application-local.yml",
                "src/main/resources/application-local.yml"));

        assertThat(text).contains("import: optional:file:${user.home}/.mutuus/${spring.application.name}/local.yml");
        assertThat(text)
                .doesNotContain("password: eva")
                .doesNotContain("GOLMOK_DB_PASSWORD")
                .doesNotContain("REDIS_PASSWORD");
    }

    @Test
    void local_secret_example_and_ignore_rules_are_present() throws IOException {
        String example = Files.readString(root("samples/sample-api/secrets/local.example.yml",
                "secrets/local.example.yml"));
        String ignore = Files.readString(root(".gitignore", "../../.gitignore"));

        assertThat(example)
                .contains("spring:")
                .contains("datasource:")
                .contains("<local database password>")
                .contains("<local redis password>");
        assertThat(ignore)
                .contains("*.decrypted.yml")
                .contains("samples/**/secrets/*.yml")
                .contains("!samples/**/secrets/*.example.yml");
    }

    @Test
    void canonical_secret_document_locks_inline_ciphertext_and_common_module_boundary() throws IOException {
        String doc = Files.readString(root("docs/260707.002.SECRET_MANAGEMENT_STANDARD.md",
                "../../docs/260707.002.SECRET_MANAGEMENT_STANDARD.md"));

        assertThat(doc)
                .contains("Runtime resolver and sample proof implemented")
                .contains("secret:v2:inline:")
                .contains("its visibility alone is not a security finding")
                .contains("existing `lib` module")
                .contains("spring.config.import")
                .contains("SecurityConfigAuditor")
                .contains("does not own")
                .contains("Runtime secret-manager SDK coupling")
                .doesNotContain("deployer /")
                .doesNotContain("admin /");
    }

    @Test
    void encrypted_secret_loader_and_sample_profile_are_wired_without_embedded_secrets() throws IOException {
        String factories = Files.readString(root("lib/src/main/resources/META-INF/spring.factories",
                "../../lib/src/main/resources/META-INF/spring.factories"));
        String profile = Files.readString(root(
                "samples/sample-api/src/main/resources/application-secret-local.yml",
                "src/main/resources/application-secret-local.yml"));

        assertThat(root(
                "lib/src/main/java/ai/mutuus/common/secret/EncryptedSecretEnvironmentPostProcessor.java",
                "../../lib/src/main/java/ai/mutuus/common/secret/EncryptedSecretEnvironmentPostProcessor.java"))
                .exists();
        assertThat(factories).contains("ai.mutuus.common.secret.EncryptedSecretEnvironmentPostProcessor");
        assertThat(profile)
                .contains("enabled: true")
                .contains("password: ${GOLMOK_DB_PASSWORD_TOKEN}")
                .contains("keystore: ${SAMPLE_SECRET_KEYSTORE}")
                .contains("keystore-password-file: ${SAMPLE_SECRET_KEYSTORE_PASSWORD_FILE}")
                .doesNotContain("secret:v2:inline:")
                .doesNotContain("BEGIN PRIVATE KEY");
    }

    private static Path root(String fromRepositoryRoot, String fromSampleModule) {
        Path rootPath = Path.of(fromRepositoryRoot);
        if (Files.exists(rootPath)) {
            return rootPath;
        }
        return Path.of(fromSampleModule);
    }
}
