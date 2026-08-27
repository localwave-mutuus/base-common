package ai.mutuus.common.secret;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Offline Secret Manager가 생산하는 엄격한 v2 레코드. */
record EncryptedSecretRecord(
        String formatVersion,
        String program,
        String environment,
        String configKey,
        String keyId,
        String algorithm,
        String encryptedDataKey,
        String nonce,
        String ciphertext,
        String createdAt) {

    static final String INLINE_PREFIX = "secret:v2:inline:";
    static final String FORMAT_VERSION = "2";
    static final String ALGORITHM = "AES-256-GCM+RSA-OAEP-SHA256-MGF1-SHA256";
    static final int MAX_DECODED_TOKEN_BYTES = 16 * 1024;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int GCM_TAG_BYTES = 16;
    private static final Set<String> FIELDS = Set.of(
            "formatVersion", "program", "environment", "configKey", "keyId",
            "algorithm", "encryptedDataKey", "nonce", "ciphertext", "createdAt");

    EncryptedSecretRecord {
        requireText(formatVersion, "formatVersion");
        requireText(program, "program");
        requireText(environment, "environment");
        requireText(configKey, "configKey");
        requireText(keyId, "keyId");
        requireText(algorithm, "algorithm");
        requireText(encryptedDataKey, "encryptedDataKey");
        requireText(nonce, "nonce");
        requireText(ciphertext, "ciphertext");
        requireText(createdAt, "createdAt");
        if (!FORMAT_VERSION.equals(formatVersion)) {
            throw new IllegalArgumentException("Only secret format version 2 is supported");
        }
        if (!ALGORITHM.equals(algorithm)) {
            throw new IllegalArgumentException("Unsupported secret algorithm");
        }
        try {
            Instant.parse(createdAt);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid createdAt timestamp", exception);
        }
        byte[] decodedKey = decodeStandardBase64(encryptedDataKey, "encryptedDataKey");
        byte[] decodedNonce = decodeStandardBase64(nonce, "nonce");
        byte[] decodedCiphertext = decodeStandardBase64(ciphertext, "ciphertext");
        try {
            if (decodedKey.length == 0 || decodedKey.length > 1024) {
                throw new IllegalArgumentException("Invalid encryptedDataKey length");
            }
            if (decodedNonce.length != GCM_NONCE_BYTES) {
                throw new IllegalArgumentException("nonce must be 12 bytes");
            }
            if (decodedCiphertext.length < GCM_TAG_BYTES) {
                throw new IllegalArgumentException("ciphertext is too short");
            }
        } finally {
            java.util.Arrays.fill(decodedKey, (byte) 0);
            java.util.Arrays.fill(decodedNonce, (byte) 0);
            java.util.Arrays.fill(decodedCiphertext, (byte) 0);
        }
    }

    static EncryptedSecretRecord parseToken(String token) {
        Objects.requireNonNull(token, "token");
        if (!token.startsWith(INLINE_PREFIX)) {
            throw new IllegalArgumentException("Unsupported encrypted secret prefix");
        }
        String encoded = token.substring(INLINE_PREFIX.length());
        if (encoded.isEmpty() || encoded.indexOf('=') >= 0 || !encoded.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Inline token must be unpadded Base64URL");
        }
        if (encoded.length() > ((MAX_DECODED_TOKEN_BYTES + 2) / 3) * 4) {
            throw new IllegalArgumentException("Inline token exceeds the size limit");
        }

        byte[] jsonBytes;
        try {
            jsonBytes = Base64.getUrlDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Inline token is not valid Base64URL", exception);
        }
        try {
            if (jsonBytes.length > MAX_DECODED_TOKEN_BYTES) {
                throw new IllegalArgumentException("Inline token exceeds the size limit");
            }
            String json = decodeUtf8(jsonBytes);
            return parseJson(json);
        } finally {
            java.util.Arrays.fill(jsonBytes, (byte) 0);
        }
    }

    private static EncryptedSecretRecord parseJson(String json) {
        Map<String, String> values = parseFlatJson(json);
        if (!values.keySet().equals(FIELDS)) {
            throw new IllegalArgumentException("Secret record field set is invalid");
        }
        return new EncryptedSecretRecord(
                values.get("formatVersion"),
                values.get("program"),
                values.get("environment"),
                values.get("configKey"),
                values.get("keyId"),
                values.get("algorithm"),
                values.get("encryptedDataKey"),
                values.get("nonce"),
                values.get("ciphertext"),
                values.get("createdAt"));
    }

    byte[] decodedEncryptedDataKey() {
        return decodeStandardBase64(encryptedDataKey, "encryptedDataKey");
    }

    byte[] decodedNonce() {
        return decodeStandardBase64(nonce, "nonce");
    }

    byte[] decodedCiphertext() {
        return decodeStandardBase64(ciphertext, "ciphertext");
    }

    byte[] associatedData() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                for (String field : new String[]{
                        formatVersion, program, environment, configKey, keyId, algorithm, createdAt}) {
                    byte[] encoded = field.getBytes(StandardCharsets.UTF_8);
                    output.writeInt(encoded.length);
                    output.write(encoded);
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("Unexpected in-memory AAD encoding failure", impossible);
        }
    }

    private static Map<String, String> parseFlatJson(String content) {
        JsonCursor cursor = new JsonCursor(content);
        Map<String, String> values = new LinkedHashMap<>();
        cursor.expect('{');
        if (cursor.consume('}')) {
            cursor.end();
            return values;
        }
        while (true) {
            String key = cursor.string();
            cursor.expect(':');
            String value = cursor.string();
            if (values.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Duplicate JSON field");
            }
            if (cursor.consume('}')) {
                cursor.end();
                return values;
            }
            cursor.expect(',');
        }
    }

    private static String decodeUtf8(byte[] value) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Secret record is not valid UTF-8", exception);
        }
    }

    private static byte[] decodeStandardBase64(String value, String field) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " is not valid Base64", exception);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static final class JsonCursor {
        private final String content;
        private int index;

        private JsonCursor(String content) {
            this.content = Objects.requireNonNull(content, "content");
        }

        private void expect(char expected) {
            whitespace();
            if (index >= content.length() || content.charAt(index) != expected) {
                throw error("Unexpected JSON token");
            }
            index++;
        }

        private boolean consume(char expected) {
            whitespace();
            if (index < content.length() && content.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private String string() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < content.length()) {
                char character = content.charAt(index++);
                if (character == '"') {
                    return value.toString();
                }
                if (character != '\\') {
                    if (character < 0x20) {
                        throw error("Unescaped control character");
                    }
                    value.append(character);
                    continue;
                }
                if (index >= content.length()) {
                    throw error("Incomplete escape sequence");
                }
                char escaped = content.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> value.append(escaped);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(unicode());
                    default -> throw error("Invalid escape sequence");
                }
            }
            throw error("Unterminated string");
        }

        private char unicode() {
            if (index + 4 > content.length()) {
                throw error("Incomplete Unicode escape");
            }
            String hex = content.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape");
            }
        }

        private void end() {
            whitespace();
            if (index != content.length()) {
                throw error("Unexpected trailing content");
            }
        }

        private void whitespace() {
            while (index < content.length() && Character.isWhitespace(content.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}
