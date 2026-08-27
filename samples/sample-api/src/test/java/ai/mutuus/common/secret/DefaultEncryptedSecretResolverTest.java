package ai.mutuus.common.secret;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultEncryptedSecretResolverTest {

    @TempDir
    Path tempDir;

    private SecretTestFixture.KeyMaterial keys;
    private DefaultEncryptedSecretResolver resolver;
    private EncryptedSecretResolver.ExpectedMetadata expected;

    @BeforeEach
    void setUp() throws Exception {
        keys = SecretTestFixture.createKeyMaterial(tempDir);
        resolver = new DefaultEncryptedSecretResolver();
        expected = new EncryptedSecretResolver.ExpectedMetadata(
                "spring.datasource.password", "sample-api", "local", "database.password",
                Set.of(SecretTestFixture.KEY_ID));
    }

    @Test
    void 생산자_호환_v2_토큰을_복호화한다() throws Exception {
        char[] plaintext = "fixture-db-password".toCharArray();
        String token = SecretTestFixture.token(keys.publicKey(), plaintext,
                "sample-api", "local", "database.password", SecretTestFixture.KEY_ID);
        char[] keyPassword = SecretTestFixture.keyPassword();
        char[] resolved = null;
        try {
            resolved = resolver.resolve(token, keys.keyStore(), keyPassword, expected);
            assertThat(resolved).containsExactly(plaintext);
        } finally {
            Arrays.fill(plaintext, '\0');
            Arrays.fill(keyPassword, '\0');
            if (resolved != null) {
                Arrays.fill(resolved, '\0');
            }
        }
    }

    @Test
    void 프로그램_환경_configKey_불일치는_거부한다() throws Exception {
        String token = token("fixture-db-password");

        for (EncryptedSecretResolver.ExpectedMetadata mismatch : new EncryptedSecretResolver.ExpectedMetadata[]{
                new EncryptedSecretResolver.ExpectedMetadata(
                        "spring.datasource.password", "other", "local", "database.password", Set.of()),
                new EncryptedSecretResolver.ExpectedMetadata(
                        "spring.datasource.password", "sample-api", "stage", "database.password", Set.of()),
                new EncryptedSecretResolver.ExpectedMetadata(
                        "spring.datasource.password", "sample-api", "local", "other.password", Set.of())}) {
            assertFailure(token, mismatch, SecretResolutionException.Reason.METADATA_MISMATCH);
        }
    }

    @Test
    void 변조된_AAD와_ciphertext는_인증_실패한다() throws Exception {
        String token = token("fixture-db-password");
        String aadTampered = SecretTestFixture.rewriteJson(token,
                json -> json.replace("\"program\":\"sample-api\"", "\"program\":\"sample-apj\""));
        EncryptedSecretResolver.ExpectedMetadata tamperedExpected =
                new EncryptedSecretResolver.ExpectedMetadata(
                        "spring.datasource.password", "sample-apj", "local", "database.password", Set.of());
        assertFailure(aadTampered, tamperedExpected,
                SecretResolutionException.Reason.CRYPTOGRAPHIC_VALIDATION_FAILED);

        String ciphertextTampered = SecretTestFixture.tamperBase64Field(token, "ciphertext");
        assertFailure(ciphertextTampered, expected,
                SecretResolutionException.Reason.CRYPTOGRAPHIC_VALIDATION_FAILED);
    }

    @Test
    void v1_unknown_duplicate_oversize_레코드는_파싱단계에서_거부한다() throws Exception {
        String token = token("fixture-db-password");
        String v1 = SecretTestFixture.rewriteJson(token,
                json -> json.replace("\"formatVersion\":\"2\"", "\"formatVersion\":\"1\""));
        String unknown = SecretTestFixture.rewriteJson(token,
                json -> json.substring(0, json.length() - 1) + ",\"unknown\":\"x\"}");
        String duplicate = SecretTestFixture.rewriteJson(token,
                json -> json.substring(0, json.length() - 1) + ",\"program\":\"sample-api\"}");
        String oversize = EncryptedSecretRecord.INLINE_PREFIX + "A".repeat(22_000);

        for (String invalid : new String[]{v1, unknown, duplicate, oversize}) {
            assertFailure(invalid, expected, SecretResolutionException.Reason.MALFORMED_TOKEN);
        }
    }

    @Test
    void 허용되지_않은_keyId와_잘못된_keystore_password를_거부한다() throws Exception {
        String token = token("fixture-db-password");
        EncryptedSecretResolver.ExpectedMetadata denied = new EncryptedSecretResolver.ExpectedMetadata(
                "spring.datasource.password", "sample-api", "local", "database.password", Set.of("other-key"));
        assertFailure(token, denied, SecretResolutionException.Reason.KEY_NOT_ALLOWED);

        char[] wrongPassword = "wrong-password".toCharArray();
        try {
            assertThatThrownBy(() -> resolver.resolve(token, keys.keyStore(), wrongPassword, expected))
                    .isInstanceOfSatisfying(SecretResolutionException.class,
                            exception -> assertThat(exception.reason())
                                    .isEqualTo(SecretResolutionException.Reason.KEYSTORE_UNAVAILABLE));
        } finally {
            Arrays.fill(wrongPassword, '\0');
        }
    }

    @Test
    void 빈_평문과_오류메시지의_비밀값_노출을_거부한다() throws Exception {
        String token = token("   ");
        char[] keyPassword = SecretTestFixture.keyPassword();
        try {
            assertThatThrownBy(() -> resolver.resolve(token, keys.keyStore(), keyPassword, expected))
                    .isInstanceOfSatisfying(SecretResolutionException.class, exception -> {
                        assertThat(exception.reason()).isEqualTo(SecretResolutionException.Reason.BLANK_PLAINTEXT);
                        assertThat(exception.getMessage())
                                .doesNotContain(token)
                                .doesNotContain("fixture-db-password")
                                .doesNotContain(new String(keyPassword));
                    });
        } finally {
            Arrays.fill(keyPassword, '\0');
        }
    }

    private String token(String value) throws Exception {
        char[] plaintext = value.toCharArray();
        try {
            return SecretTestFixture.token(keys.publicKey(), plaintext,
                    "sample-api", "local", "database.password", SecretTestFixture.KEY_ID);
        } finally {
            Arrays.fill(plaintext, '\0');
        }
    }

    private void assertFailure(String token, EncryptedSecretResolver.ExpectedMetadata metadata,
                               SecretResolutionException.Reason reason) {
        char[] keyPassword = SecretTestFixture.keyPassword();
        try {
            assertThatThrownBy(() -> resolver.resolve(token, keys.keyStore(), keyPassword, metadata))
                    .isInstanceOfSatisfying(SecretResolutionException.class,
                            exception -> assertThat(exception.reason()).isEqualTo(reason));
        } finally {
            Arrays.fill(keyPassword, '\0');
        }
    }
}
