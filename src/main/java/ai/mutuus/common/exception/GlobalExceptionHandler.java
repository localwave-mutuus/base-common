package ai.mutuus.common.exception;

import java.net.URI;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.common.logging.AccessLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC7807 ProblemDetail 기반 전역 예외 처리.
 * <p>다국어 메시지(common-i18n)와 추적ID(common-core)를 응답에 함께 담고,
 * 오류 발생 시점을 {@link AccessLogger}로 자동 로깅한다(비즈니스=WARN, 서버=ERROR).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageResolver messages;
    private final AccessLogger accessLogger;

    public GlobalExceptionHandler(MessageResolver messages, AccessLogger accessLogger) {
        this.messages = messages;
        this.accessLogger = accessLogger;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        String detail = messages.get(code.messageKey(), ex.getArgs());
        accessLogger.businessError(code.name(), request.getRequestURI(), detail);
        return build(code.status(), code.name(), detail);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        accessLogger.serverError(request.getRequestURI(), ex);
        return build(code.status(), code.name(),
                messages.get(code.messageKey()));
    }

    private ProblemDetail build(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("urn:mutuus:error:" + code.toLowerCase()));
        pd.setTitle(code);
        pd.setProperty("traceId", TraceContext.traceId());
        pd.setProperty("screenId", TraceContext.get(HeaderNames.SCREEN_ID).orElse(null));
        pd.setProperty("timestamp", java.time.OffsetDateTime.now().toString());
        return pd;
    }
}
