package ai.mutuus.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * File-level guard for the external secret-file standard.
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
    void canonical_secret_document_describes_common_module_boundary() throws IOException {
        String doc = Files.readString(root("docs/260707.002.SECRET_MANAGEMENT_STANDARD.md",
                "../../docs/260707.002.SECRET_MANAGEMENT_STANDARD.md"));

        assertThat(doc)
                .contains("spring.config.import")
                .contains("SecurityConfigAuditor")
                .contains("does not own")
                .contains("Runtime secret-manager SDK coupling")
                .doesNotContain("deployer /")
                .doesNotContain("admin /");
    }

    private static Path root(String fromRepositoryRoot, String fromSampleModule) {
        Path rootPath = Path.of(fromRepositoryRoot);
        if (Files.exists(rootPath)) {
            return rootPath;
        }
        return Path.of(fromSampleModule);
    }
}
