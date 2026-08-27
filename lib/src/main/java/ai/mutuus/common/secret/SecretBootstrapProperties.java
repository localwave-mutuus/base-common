package ai.mutuus.common.secret;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.env.ConfigurableEnvironment;

/** 컨텍스트 생성 전에 필요한 최소 시크릿 부트스트랩 설정. */
record SecretBootstrapProperties(
        String program,
        String environment,
        Path keyStore,
        Path keyStorePasswordFile,
        Set<String> allowedKeyIds) {

    private static final String PREFIX = "mutuus.common.secret.";

    static boolean isEnabled(ConfigurableEnvironment environment) {
        return environment.getProperty(PREFIX + "enabled", Boolean.class, false);
    }

    static SecretBootstrapProperties from(ConfigurableEnvironment environment, String target) {
        try {
            String program = required(environment, "spring.application.name");
            String secretEnvironment = required(environment, PREFIX + "environment");
            Path keyStore = filePath(required(environment, PREFIX + "keystore"));
            Path passwordFile = filePath(required(environment, PREFIX + "keystore-password-file"));
            Set<String> allowed = commaSeparated(environment.getProperty(PREFIX + "allowed-key-ids"));
            return new SecretBootstrapProperties(program, secretEnvironment, keyStore, passwordFile, allowed);
        } catch (SecretResolutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SecretResolutionException(
                    target, SecretResolutionException.Reason.MISSING_CONFIGURATION, exception);
        }
    }

    char[] readKeyStorePassword(String target) {
        byte[] encoded = null;
        CharBuffer decoded = null;
        try {
            encoded = Files.readAllBytes(keyStorePasswordFile);
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded));
            char[] raw = new char[decoded.remaining()];
            decoded.get(raw);
            int length = raw.length;
            while (length > 0 && (raw[length - 1] == '\r' || raw[length - 1] == '\n')) {
                length--;
            }
            for (int index = 0; index < length; index++) {
                if (raw[index] == '\r' || raw[index] == '\n' || raw[index] == '\0') {
                    Arrays.fill(raw, '\0');
                    throw new IOException("Bootstrap password must be one non-empty line");
                }
            }
            if (length == 0) {
                Arrays.fill(raw, '\0');
                throw new IOException("Bootstrap password is empty");
            }
            char[] result = Arrays.copyOf(raw, length);
            Arrays.fill(raw, '\0');
            return result;
        } catch (IOException exception) {
            throw new SecretResolutionException(
                    target, SecretResolutionException.Reason.KEYSTORE_UNAVAILABLE, exception);
        } finally {
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
            if (decoded != null) {
                decoded.rewind();
                while (decoded.hasRemaining()) {
                    decoded.put('\0');
                }
            }
        }
    }

    private static String required(ConfigurableEnvironment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required bootstrap property: " + name);
        }
        return environment.resolveRequiredPlaceholders(value).trim();
    }

    private static Path filePath(String locator) throws IOException {
        if (locator.startsWith("classpath:") || locator.startsWith("http:" ) || locator.startsWith("https:")) {
            throw new IOException("Only filesystem secret locators are supported");
        }
        if (locator.startsWith("file:")) {
            String fileName = locator.substring("file:".length());
            if (fileName.isBlank()) {
                throw new IOException("Secret file locator is empty");
            }
            return Path.of(fileName).toAbsolutePath().normalize();
        }
        return Path.of(locator).toAbsolutePath().normalize();
    }

    private static Set<String> commaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            if (!item.isBlank()) {
                result.add(item.trim());
            }
        }
        return Set.copyOf(result);
    }
}
