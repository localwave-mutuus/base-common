package ai.mutuus.common.secret;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Config Data 로딩 뒤, DataSource/Flyway/Hikari 구성 전에 v2 inline 시크릿을 복호화한다.
 */
public final class EncryptedSecretEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String DECRYPTED_PROPERTY_SOURCE = "mutuusDecryptedSecrets";
    private static final Map<String, String> ALLOWED_TARGETS = Map.of(
            "spring.datasource.password", "database.password");

    private final EncryptedSecretResolver resolver;
    private final Log logger;

    public EncryptedSecretEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this(new DefaultEncryptedSecretResolver(), logFactory.getLog(EncryptedSecretEnvironmentPostProcessor.class));
    }

    EncryptedSecretEnvironmentPostProcessor(EncryptedSecretResolver resolver, Log logger) {
        this.resolver = resolver;
        this.logger = logger;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!SecretBootstrapProperties.isEnabled(environment)) {
            return;
        }

        Map<String, Object> decrypted = new LinkedHashMap<>();
        Map<String, EncryptedSecretRecord> resolvedRecords = new LinkedHashMap<>();
        List<char[]> temporarySecrets = new ArrayList<>();
        String currentTarget = "spring.datasource.password";
        try {
            SecretBootstrapProperties bootstrap = SecretBootstrapProperties.from(environment, currentTarget);
            char[] keyPassword = bootstrap.readKeyStorePassword(currentTarget);
            try {
                for (Map.Entry<String, String> mapping : ALLOWED_TARGETS.entrySet()) {
                    currentTarget = mapping.getKey();
                    String token = singleEncryptedDefinition(environment, currentTarget);
                    EncryptedSecretRecord record;
                    try {
                        record = EncryptedSecretRecord.parseToken(token);
                    } catch (RuntimeException exception) {
                        throw new SecretResolutionException(currentTarget,
                                SecretResolutionException.Reason.MALFORMED_TOKEN, exception);
                    }
                    EncryptedSecretResolver.ExpectedMetadata expected =
                            new EncryptedSecretResolver.ExpectedMetadata(
                                    currentTarget,
                                    bootstrap.program(),
                                    bootstrap.environment(),
                                    mapping.getValue(),
                                    bootstrap.allowedKeyIds());
                    char[] secret = resolver.resolve(token, bootstrap.keyStore(), keyPassword, expected);
                    temporarySecrets.add(secret);
                    decrypted.put(currentTarget, new String(secret));
                    resolvedRecords.put(currentTarget, record);
                }
            } finally {
                Arrays.fill(keyPassword, '\0');
            }

            environment.getPropertySources().addFirst(
                    new MapPropertySource(DECRYPTED_PROPERTY_SOURCE, decrypted));
            resolvedRecords.forEach((target, record) ->
                    logger.info("암호화 시크릿 로딩 성공 [target=" + target
                            + ", keyId=" + record.keyId()
                            + ", formatVersion=" + record.formatVersion() + "]"));
        } catch (SecretResolutionException exception) {
            logger.error("암호화 시크릿 로딩 실패 [target=" + exception.targetProperty()
                    + ", reason=" + exception.reason() + "]");
            throw exception;
        } finally {
            for (char[] secret : temporarySecrets) {
                Arrays.fill(secret, '\0');
            }
        }
    }

    private static String singleEncryptedDefinition(ConfigurableEnvironment environment, String target) {
        List<String> definitions = new ArrayList<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (DECRYPTED_PROPERTY_SOURCE.equals(source.getName())
                    || "configurationProperties".equals(source.getName())) {
                continue;
            }
            Object value = source.getProperty(target);
            if (value == null || value.toString().isBlank()) {
                continue;
            }
            try {
                definitions.add(environment.resolveRequiredPlaceholders(value.toString()));
            } catch (RuntimeException exception) {
                throw new SecretResolutionException(target,
                        SecretResolutionException.Reason.MISSING_CONFIGURATION, exception);
            }
        }
        if (definitions.isEmpty()) {
            throw new SecretResolutionException(target,
                    SecretResolutionException.Reason.MISSING_CONFIGURATION);
        }
        if (definitions.size() != 1) {
            throw new SecretResolutionException(target,
                    SecretResolutionException.Reason.PROPERTY_SOURCE_CONFLICT);
        }
        String token = definitions.getFirst();
        if (!token.startsWith(EncryptedSecretRecord.INLINE_PREFIX)) {
            throw new SecretResolutionException(target,
                    SecretResolutionException.Reason.UNSUPPORTED_VALUE);
        }
        return token;
    }

    @Override
    public int getOrder() {
        // ConfigDataEnvironmentPostProcessor 이후, 공통 기본값 주입 processor 바로 전에 실행한다.
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
