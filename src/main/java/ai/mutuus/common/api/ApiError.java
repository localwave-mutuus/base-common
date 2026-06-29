package ai.mutuus.common.api;

import java.util.List;

/**
 * {@link ApiResponse}의 오류 상세. 익셉션/검증 실패 정보를 수용한다.
 *
 * @param code        안정적 오류 코드(예: VALIDATION_ERROR) — 클라이언트 분기용
 * @param detail      사람이 읽는 오류 메시지(다국어 적용 결과)
 * @param fieldErrors 검증 실패 시 필드별 사유(없으면 빈 목록)
 * @param exception   예외 클래스명(서버 오류 진단용, 운영 노출 정책에 따라 null 가능)
 */
public record ApiError(String code, String detail, List<FieldError> fieldErrors, String exception) {

    public static ApiError of(String code, String detail) {
        return new ApiError(code, detail, List.of(), null);
    }

    public static ApiError of(String code, String detail, List<FieldError> fieldErrors) {
        return new ApiError(code, detail, fieldErrors == null ? List.of() : fieldErrors, null);
    }

    public ApiError withException(String exceptionClassName) {
        return new ApiError(code, detail, fieldErrors, exceptionClassName);
    }

    /** 검증 실패의 필드 단위 사유. */
    public record FieldError(String field, String reason) {
    }
}
