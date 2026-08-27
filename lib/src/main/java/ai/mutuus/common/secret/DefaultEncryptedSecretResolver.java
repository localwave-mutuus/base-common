package ai.mutuus.common.secret;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/** JDK 암호화 API만 사용하는 Offline Secret Manager v2 resolver. */
final class DefaultEncryptedSecretResolver implements EncryptedSecretResolver {

    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    @Override
    public char[] resolve(String inlineToken, Path keyStorePath, char[] keyPassword,
                          ExpectedMetadata expected) throws SecretResolutionException {
        EncryptedSecretRecord record;
        try {
            record = EncryptedSecretRecord.parseToken(inlineToken);
        } catch (RuntimeException exception) {
            throw failure(expected.targetProperty(), SecretResolutionException.Reason.MALFORMED_TOKEN, exception);
        }

        validateMetadata(record, expected);
        if (!expected.allowedKeyIds().isEmpty() && !expected.allowedKeyIds().contains(record.keyId())) {
            throw failure(expected.targetProperty(), SecretResolutionException.Reason.KEY_NOT_ALLOWED, null);
        }

        PrivateKey privateKey = loadPrivateKey(keyStorePath, keyPassword, record.keyId(), expected.targetProperty());
        return decrypt(record, privateKey, expected.targetProperty());
    }

    private static void validateMetadata(EncryptedSecretRecord record, ExpectedMetadata expected) {
        if (!record.program().equals(expected.program())
                || !record.environment().equals(expected.environment())
                || !record.configKey().equals(expected.configKey())) {
            throw failure(expected.targetProperty(), SecretResolutionException.Reason.METADATA_MISMATCH, null);
        }
    }

    private static PrivateKey loadPrivateKey(Path path, char[] password, String keyId, String target) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream input = Files.newInputStream(path)) {
                keyStore.load(input, password);
            }
            if (!keyStore.isKeyEntry(keyId)) {
                throw failure(target, SecretResolutionException.Reason.KEY_NOT_ALLOWED, null);
            }
            Key key = keyStore.getKey(keyId, password);
            if (!(key instanceof PrivateKey privateKey) || !(privateKey instanceof RSAKey)) {
                throw failure(target, SecretResolutionException.Reason.KEY_NOT_ALLOWED, null);
            }
            return privateKey;
        } catch (SecretResolutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failure(target, SecretResolutionException.Reason.KEYSTORE_UNAVAILABLE, exception);
        }
    }

    private static char[] decrypt(EncryptedSecretRecord record, PrivateKey privateKey, String target) {
        byte[] encryptedDataKey = record.decodedEncryptedDataKey();
        byte[] nonce = record.decodedNonce();
        byte[] ciphertext = record.decodedCiphertext();
        byte[] associatedData = record.associatedData();
        byte[] dataKey = null;
        byte[] plaintext = null;
        try {
            int rsaBytes = ((((RSAKey) privateKey).getModulus().bitLength()) + 7) / 8;
            if (encryptedDataKey.length != rsaBytes) {
                throw failure(target, SecretResolutionException.Reason.CRYPTOGRAPHIC_VALIDATION_FAILED, null);
            }

            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SHA_256);
            dataKey = rsa.doFinal(encryptedDataKey);
            if (dataKey.length != 32) {
                throw failure(target, SecretResolutionException.Reason.CRYPTOGRAPHIC_VALIDATION_FAILED, null);
            }

            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dataKey, "AES"), new GCMParameterSpec(128, nonce));
            aes.updateAAD(associatedData);
            plaintext = aes.doFinal(ciphertext);
            char[] decoded = decodeUtf8(plaintext, target);
            if (isBlank(decoded)) {
                Arrays.fill(decoded, '\0');
                throw failure(target, SecretResolutionException.Reason.BLANK_PLAINTEXT, null);
            }
            return decoded;
        } catch (SecretResolutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failure(target, SecretResolutionException.Reason.CRYPTOGRAPHIC_VALIDATION_FAILED, exception);
        } finally {
            Arrays.fill(encryptedDataKey, (byte) 0);
            Arrays.fill(nonce, (byte) 0);
            Arrays.fill(ciphertext, (byte) 0);
            Arrays.fill(associatedData, (byte) 0);
            if (dataKey != null) {
                Arrays.fill(dataKey, (byte) 0);
            }
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    private static char[] decodeUtf8(byte[] plaintext, String target) {
        CharBuffer characters = null;
        try {
            characters = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(plaintext));
            char[] result = new char[characters.remaining()];
            characters.get(result);
            return result;
        } catch (CharacterCodingException exception) {
            throw failure(target, SecretResolutionException.Reason.CRYPTOGRAPHIC_VALIDATION_FAILED, exception);
        } finally {
            if (characters != null) {
                characters.rewind();
                while (characters.hasRemaining()) {
                    characters.put('\0');
                }
            }
        }
    }

    private static boolean isBlank(char[] value) {
        for (char character : value) {
            if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return true;
    }

    private static SecretResolutionException failure(
            String target, SecretResolutionException.Reason reason, Throwable cause) {
        return new SecretResolutionException(target, reason, cause);
    }
}
