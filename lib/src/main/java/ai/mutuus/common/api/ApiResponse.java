package ai.mutuus.common.api;

import java.time.OffsetDateTime;
import java.util.List;

import ai.mutuus.common.core.TraceContext;

/**
 * 모든 API 통신의 표준 리턴 객체(성공/오류 통합 봉투).
 * <p>성공/오류 모두 동일한 구조를 가지며, 클라이언트는 {@link #code}로 분기한다
 * (성공은 {@code "OK"}). 오류 상세는 {@link #error}에 담긴다. HTTP 상태코드는 별도로
 * 정확히 설정되며(예: 404), 본문 구조는 항상 동일하다.
 * <p>{@link #traceId}/{@link #timestamp}로 응답 자체가 추적·시점 정보를 갖는다(육하원칙의 when/where).
 *
 * @param <T> 성공 페이로드 타입
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        ApiError error,
        String traceId,
        OffsetDateTime timestamp) {

    private static final String OK_CODE = "OK";

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "OK");
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(OK_CODE, message, data, null, TraceContext.traceId(), OffsetDateTime.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return error(code, message, ApiError.of(code, message));
    }

    public static ApiResponse<Void> error(String code, String message, List<ApiError.FieldError> fieldErrors) {
        return error(code, message, ApiError.of(code, message, fieldErrors));
    }

    public static ApiResponse<Void> error(String code, String message, ApiError error) {
        return new ApiResponse<>(code, message, null, error, TraceContext.traceId(), OffsetDateTime.now());
    }

    public boolean isSuccess() {
        return OK_CODE.equals(code);
    }
}
