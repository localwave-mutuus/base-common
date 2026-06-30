package ai.mutuus.common.exception;

/**
 * 비즈니스 예외 기본 타입. 에러 코드와 메시지 인자를 보관한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.messageKey());
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
