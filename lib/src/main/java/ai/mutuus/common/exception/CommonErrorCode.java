package ai.mutuus.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 라이브러리가 제공하는 공통 에러 코드({@link ErrorCode} 기본 구현).
 * enum 이름이 곧 안정적 오류 코드 문자열({@code code})이며, {@code messageKey}는 common-i18n
 * 메시지 번들 키와 매핑된다. 다양한 API 오류 케이스를 망라한다 — 요청/검증, 인증·인가, 자원 상태,
 * 트래픽, 서버·외부 연동 장애. <b>소비 서비스는 이 enum 을 확장하지 말고</b>(enum 은 확장 불가)
 * {@link ErrorCode} 인터페이스를 구현한 자체 enum 으로 도메인 코드를 추가한다.
 */
public enum CommonErrorCode implements ErrorCode {

    // --- 4xx: 클라이언트 요청 문제 ---
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "error.invalid.request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "error.validation"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "error.malformed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "error.forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "error.not.found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "error.method.not.allowed"),
    CONFLICT(HttpStatus.CONFLICT, "error.conflict"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "error.unsupported.media.type"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "error.too.many.requests"),

    // --- 5xx: 서버·외부 연동 장애 ---
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal"),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "error.external.api"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "error.service.unavailable"),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "error.gateway.timeout");

    private final HttpStatus status;
    private final String messageKey;

    CommonErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }

    /** 안정적 오류 코드 문자열(= enum 이름). 클라이언트 분기/로깅에 사용. */
    @Override
    public String code() {
        return name();
    }
}
