package ai.mutuus.common.exception;

import java.net.URI;

import ai.mutuus.common.core.HeaderNames;
import ai.mutuus.common.core.TraceContext;
import ai.mutuus.common.i18n.MessageResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * RFC7807 ProblemDetail 기반 전역 예외 처리.
 * <p>다국어 메시지(common-i18n)와 추적ID(common-core)를 응답에 함께 담는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageResolver messages;

    public GlobalExceptionHandler(MessageResolver messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, WebRequest request) {
        ErrorCode code = ex.getErrorCode();
        return build(code.status(), code.name(),
                messages.get(code.messageKey(), ex.getArgs()));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
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
