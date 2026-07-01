package ai.mutuus.sample.demo;

import ai.mutuus.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * <b>소비 서비스가 직접 정의한 도메인 에러 코드</b> — 라이브러리의 {@link ErrorCode} 인터페이스를 구현한다.
 * 라이브러리 {@code CommonErrorCode} 를 건드리지 않고도, 이 코드를 {@code BusinessException} 에 실으면
 * {@code GlobalExceptionHandler} 가 공통 코드와 <b>동일한 표준 봉투/흐름</b>으로 처리한다
 * (상태·code·i18n 메시지·traceId). 메시지 키는 소비자 자체 번들(messages/sample-messages)에서 해석된다.
 */
public enum SampleErrorCode implements ErrorCode {

    /** 이미 배송이 시작된 주문에 대한 취소/변경 시도 — 도메인 충돌(409). */
    ORDER_ALREADY_SHIPPED(HttpStatus.CONFLICT, "error.sample.order.shipped");

    private final HttpStatus status;
    private final String messageKey;

    SampleErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
