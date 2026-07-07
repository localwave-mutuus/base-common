package ai.mutuus.common.exception;

import java.util.List;

import ai.mutuus.common.api.ApiError;
import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.common.logging.AccessLogger;
import java.net.SocketTimeoutException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 처리. 모든 예외를 표준 {@link ApiResponse} 봉투(통합 형식)로 변환하고,
 * 발생 시점을 {@link AccessLogger}로 자동 로깅한다(비즈니스/검증=WARN, 서버=ERROR).
 * <p>다국어 메시지(common-i18n)와 추적ID(common-core)를 함께 담는다. HTTP 상태코드는
 * {@link ErrorCode}에서 정확히 설정되며, 본문 구조는 성공/오류가 동일하다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageResolver messages;
    private final AccessLogger accessLogger;
    /** 오류 응답에 예외 클래스명을 노출할지(운영 기본 false). 예외/스택은 항상 로그에만 남긴다. */
    private final boolean exposeException;

    public GlobalExceptionHandler(MessageResolver messages, AccessLogger accessLogger) {
        this(messages, accessLogger, false);
    }

    public GlobalExceptionHandler(MessageResolver messages, AccessLogger accessLogger, boolean exposeException) {
        this.messages = messages;
        this.accessLogger = accessLogger;
        this.exposeException = exposeException;
    }

    /** 애플리케이션이 던진 비즈니스 예외. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        String detail = messages.get(code.messageKey(), ex.getArgs());
        return clientError(code, detail, List.of(), request);
    }

    /** 요청 본문 검증 실패(@Valid). 필드별 사유를 함께 반환한다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return clientError(CommonErrorCode.VALIDATION_ERROR,
                messages.get(CommonErrorCode.VALIDATION_ERROR.messageKey()), fieldErrors, request);
    }

    /** 허용되지 않은 HTTP 메서드. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                    HttpServletRequest request) {
        return clientError(CommonErrorCode.METHOD_NOT_ALLOWED,
                messages.get(CommonErrorCode.METHOD_NOT_ALLOWED.messageKey()), List.of(), request);
    }

    /** 지원하지 않는 미디어 타입. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                             HttpServletRequest request) {
        return clientError(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE,
                messages.get(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.messageKey()), List.of(), request);
    }

    /** 매핑되지 않은 경로/정적 리소스 없음. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex,
                                                            HttpServletRequest request) {
        return clientError(CommonErrorCode.NOT_FOUND,
                messages.get(CommonErrorCode.NOT_FOUND.messageKey()), List.of(), request);
    }

    /** 잘못된 포맷으로 들어온 요청 본문(파싱 불가 JSON 등) → 400 MALFORMED. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformed(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        return clientError(CommonErrorCode.MALFORMED_REQUEST,
                messages.get(CommonErrorCode.MALFORMED_REQUEST.messageKey()), List.of(), request);
    }

    /**
     * 아웃바운드 호출의 네트워크 에러(연결 실패/타임아웃 등). RestClient/RestTemplate I/O 실패는
     * {@link ResourceAccessException} 로 래핑된다. 타임아웃은 504, 그 외 연결 오류는 502 로 매핑하고
     * 스택을 포함해 ERROR 로깅한다.
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleNetwork(ResourceAccessException ex,
                                                           HttpServletRequest request) {
        ErrorCode code = isTimeout(ex)
                ? CommonErrorCode.GATEWAY_TIMEOUT : CommonErrorCode.EXTERNAL_API_ERROR;
        accessLogger.serverError(request.getRequestURI(), code.code(), ex);
        String detail = messages.get(code.messageKey());
        return ResponseEntity.status(code.status()).body(ApiResponse.error(code.code(), detail, errorOf(code, detail, ex)));
    }

    /** 타임아웃 판별 — HttpURLConnection 계열(SocketTimeout)·JDK HttpClient 계열(HttpTimeout) 모두 커버. */
    private static boolean isTimeout(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /** 그 외 처리되지 않은 모든 예외 → 500. 스택을 포함해 ERROR 로깅. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        ErrorCode code = CommonErrorCode.INTERNAL_ERROR;
        accessLogger.serverError(request.getRequestURI(), code.code(), ex);
        String detail = messages.get(code.messageKey());
        return ResponseEntity.status(code.status()).body(ApiResponse.error(code.code(), detail, errorOf(code, detail, ex)));
    }

    /** 오류 상세 생성 — 예외 클래스명은 노출 정책({@code exposeException})이 true 일 때만 응답에 싣는다(기본 미노출). */
    private ApiError errorOf(ErrorCode code, String detail, Throwable ex) {
        ApiError error = ApiError.of(code.code(), detail);
        return exposeException ? error.withException(ex.getClass().getName()) : error;
    }

    /** 4xx 공통 처리: WARN 로깅 + 표준 봉투 반환. */
    private ResponseEntity<ApiResponse<Void>> clientError(ErrorCode code, String detail,
                                                          List<ApiError.FieldError> fieldErrors,
                                                          HttpServletRequest request) {
        accessLogger.businessError(code.code(), request.getRequestURI(), detail);
        ApiResponse<Void> body = ApiResponse.error(code.code(), detail, ApiError.of(code.code(), detail, fieldErrors));
        return ResponseEntity.status(code.status()).body(body);
    }
}
