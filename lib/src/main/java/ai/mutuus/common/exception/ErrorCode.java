package ai.mutuus.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 표준 에러 코드 계약. 코드 문자열·HTTP 상태·i18n 메시지 키를 노출한다.
 * <p>라이브러리 기본 코드는 {@link CommonErrorCode} 가 구현하고, <b>소비 서비스는 자체 enum 이
 * 이 인터페이스를 구현</b>해 도메인 에러 코드를 추가할 수 있다. 그렇게 만든 코드는 같은 타입으로
 * {@link BusinessException} 에 실려 {@code GlobalExceptionHandler} 가 라이브러리 코드와 <b>동일하게</b>
 * 표준 {@code ApiResponse} 봉투로 변환한다(상태·코드·i18n 메시지·traceId).
 * <pre>{@code
 * public enum OrderErrorCode implements ErrorCode {
 *     ALREADY_SHIPPED(HttpStatus.CONFLICT, "error.order.already.shipped");
 *     private final HttpStatus status; private final String messageKey;
 *     OrderErrorCode(HttpStatus s, String k) { this.status = s; this.messageKey = k; }
 *     public String code() { return name(); }
 *     public HttpStatus status() { return status; }
 *     public String messageKey() { return messageKey; }
 * }
 * }</pre>
 */
public interface ErrorCode {

    /** 안정적 오류 코드 문자열(클라이언트 분기/로깅에 사용). enum 이면 보통 {@code name()}. */
    String code();

    /** 응답 HTTP 상태. */
    HttpStatus status();

    /** i18n 메시지 번들 키(소비자 커스텀 코드는 자신의 메시지 번들 키를 반환하면 된다). */
    String messageKey();
}
