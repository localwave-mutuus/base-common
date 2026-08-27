package ai.mutuus.common.secret;

/**
 * 애플리케이션 시작 단계에서 암호화 시크릿을 안전하게 해석하지 못했음을 나타낸다.
 *
 * <p>예외 메시지는 대상 프로퍼티와 실패 유형만 포함한다. 암호문, 평문, 키 저장소
 * 비밀번호는 어떤 경우에도 메시지에 포함하지 않는다.
 */
public final class SecretResolutionException extends IllegalStateException {

    public enum Reason {
        MISSING_CONFIGURATION,
        PROPERTY_SOURCE_CONFLICT,
        UNSUPPORTED_VALUE,
        MALFORMED_TOKEN,
        METADATA_MISMATCH,
        KEY_NOT_ALLOWED,
        KEYSTORE_UNAVAILABLE,
        CRYPTOGRAPHIC_VALIDATION_FAILED,
        BLANK_PLAINTEXT
    }

    private final String targetProperty;
    private final Reason reason;

    public SecretResolutionException(String targetProperty, Reason reason) {
        this(targetProperty, reason, null);
    }

    public SecretResolutionException(String targetProperty, Reason reason, Throwable cause) {
        super("암호화 시크릿 해석 실패 [target=" + targetProperty + ", reason=" + reason + "]", cause);
        this.targetProperty = targetProperty;
        this.reason = reason;
    }

    public String targetProperty() {
        return targetProperty;
    }

    public Reason reason() {
        return reason;
    }
}
