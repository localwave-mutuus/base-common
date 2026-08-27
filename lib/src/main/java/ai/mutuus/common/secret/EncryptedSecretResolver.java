package ai.mutuus.common.secret;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/** Offline Secret Manager v2 inline 레코드를 복호화하는 공통 계약이다. */
public interface EncryptedSecretResolver {

    /**
     * 암호화 토큰을 검증하고 평문을 반환한다.
     *
     * <p>호출자는 반환 배열과 {@code keyPassword}를 사용 직후 반드시 지워야 한다.
     */
    char[] resolve(String inlineToken, Path keyStore, char[] keyPassword,
                   ExpectedMetadata expected) throws SecretResolutionException;

    /** 암호문에 결박되어야 하는 소비 애플리케이션 메타데이터다. */
    record ExpectedMetadata(
            String targetProperty,
            String program,
            String environment,
            String configKey,
            Set<String> allowedKeyIds) {

        public ExpectedMetadata {
            targetProperty = requireText(targetProperty, "targetProperty");
            program = requireText(program, "program");
            environment = requireText(environment, "environment");
            configKey = requireText(configKey, "configKey");
            allowedKeyIds = Set.copyOf(Objects.requireNonNull(allowedKeyIds, "allowedKeyIds"));
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.trim();
        }
    }
}
