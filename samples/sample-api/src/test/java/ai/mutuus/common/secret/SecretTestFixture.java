package ai.mutuus.common.secret;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/** 테스트마다 임시 PKCS#12와 생산자 호환 v2 토큰을 생성한다. 개인키는 Git에 남지 않는다. */
final class SecretTestFixture {

    static final String KEY_ID = "test-encryption-key";
    private static final char[] KEY_PASSWORD = "test-only-keystore-password".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private SecretTestFixture() {
    }

    static KeyMaterial createKeyMaterial(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path keyStore = directory.resolve("test-key.p12");
        Path passwordFile = directory.resolve("test-key-password");
        Files.writeString(passwordFile, new String(KEY_PASSWORD), StandardCharsets.UTF_8);

        Path keytool = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "keytool.exe" : "keytool");
        Process process = new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias", KEY_ID,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA256withRSA",
                "-storetype", "PKCS12",
                "-keystore", keyStore.toString(),
                "-storepass:file", passwordFile.toString(),
                "-keypass:file", passwordFile.toString(),
                "-dname", "CN=common-platform-test",
                "-validity", "2",
                "-noprompt")
                .redirectErrorStream(true)
                .start();
        byte[] output = process.getInputStream().readAllBytes();
        int exit = process.waitFor();
        try {
            if (exit != 0) {
                throw new IllegalStateException("keytool failed with exit code " + exit);
            }
        } finally {
            Arrays.fill(output, (byte) 0);
        }

        KeyStore store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(keyStore)) {
            store.load(input, KEY_PASSWORD);
        }
        PublicKey publicKey = store.getCertificate(KEY_ID).getPublicKey();
        return new KeyMaterial(keyStore, passwordFile, publicKey);
    }

    static String token(PublicKey publicKey, char[] plaintext,
                        String program, String environment, String configKey, String keyId) throws Exception {
        String createdAt = Instant.now().toString();
        byte[] associatedData = associatedData(
                "2", program, environment, configKey, keyId,
                "AES-256-GCM+RSA-OAEP-SHA256-MGF1-SHA256", createdAt);
        byte[] plaintextBytes = encodeUtf8(plaintext);
        byte[] nonce = new byte[12];
        byte[] dataKey = null;
        byte[] encryptedDataKey = null;
        byte[] ciphertext = null;
        try {
            RANDOM.nextBytes(nonce);
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256, RANDOM);
            SecretKey secretKey = generator.generateKey();
            dataKey = secretKey.getEncoded();

            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, nonce));
            aes.updateAAD(associatedData);
            ciphertext = aes.doFinal(plaintextBytes);

            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA_256);
            encryptedDataKey = rsa.doFinal(dataKey);

            String json = "{" +
                    field("formatVersion", "2") + "," +
                    field("program", program) + "," +
                    field("environment", environment) + "," +
                    field("configKey", configKey) + "," +
                    field("keyId", keyId) + "," +
                    field("algorithm", "AES-256-GCM+RSA-OAEP-SHA256-MGF1-SHA256") + "," +
                    field("encryptedDataKey", Base64.getEncoder().encodeToString(encryptedDataKey)) + "," +
                    field("nonce", Base64.getEncoder().encodeToString(nonce)) + "," +
                    field("ciphertext", Base64.getEncoder().encodeToString(ciphertext)) + "," +
                    field("createdAt", createdAt) + "}";
            return EncryptedSecretRecord.INLINE_PREFIX
                    + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } finally {
            Arrays.fill(associatedData, (byte) 0);
            Arrays.fill(plaintextBytes, (byte) 0);
            Arrays.fill(nonce, (byte) 0);
            if (dataKey != null) {
                Arrays.fill(dataKey, (byte) 0);
            }
            if (encryptedDataKey != null) {
                Arrays.fill(encryptedDataKey, (byte) 0);
            }
            if (ciphertext != null) {
                Arrays.fill(ciphertext, (byte) 0);
            }
        }
    }

    static String rewriteJson(String token, java.util.function.UnaryOperator<String> rewrite) {
        byte[] decoded = Base64.getUrlDecoder().decode(token.substring(EncryptedSecretRecord.INLINE_PREFIX.length()));
        try {
            String changed = rewrite.apply(new String(decoded, StandardCharsets.UTF_8));
            return EncryptedSecretRecord.INLINE_PREFIX
                    + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(changed.getBytes(StandardCharsets.UTF_8));
        } finally {
            Arrays.fill(decoded, (byte) 0);
        }
    }

    static String tamperBase64Field(String token, String field) {
        return rewriteJson(token, json -> {
            String marker = "\"" + field + "\":\"";
            int start = json.indexOf(marker);
            if (start < 0) {
                throw new IllegalArgumentException("Missing fixture field: " + field);
            }
            start += marker.length();
            int end = json.indexOf('"', start);
            byte[] decoded = Base64.getDecoder().decode(json.substring(start, end));
            try {
                decoded[decoded.length - 1] ^= 0x01;
                String changed = Base64.getEncoder().encodeToString(decoded);
                return json.substring(0, start) + changed + json.substring(end);
            } finally {
                Arrays.fill(decoded, (byte) 0);
            }
        });
    }

    static char[] keyPassword() {
        return KEY_PASSWORD.clone();
    }

    private static byte[] associatedData(String... fields) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            for (String field : fields) {
                byte[] encoded = field.getBytes(StandardCharsets.UTF_8);
                output.writeInt(encoded.length);
                output.write(encoded);
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] encodeUtf8(char[] value) throws Exception {
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(value));
        byte[] result = new byte[encoded.remaining()];
        encoded.get(result);
        if (encoded.hasArray()) {
            Arrays.fill(encoded.array(), (byte) 0);
        }
        return result;
    }

    private static String field(String name, String value) {
        return "\"" + name + "\":\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    record KeyMaterial(Path keyStore, Path passwordFile, PublicKey publicKey) {
    }
}
