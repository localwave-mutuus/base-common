package ai.mutuus.common.security;

import ai.mutuus.common.api.ApiError;
import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.exception.CommonErrorCode;
import ai.mutuus.common.exception.ErrorCode;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.common.security.audit.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 메서드 보안({@code @PreAuthorize} 등)이 <b>컨트롤러 실행 중</b> 던지는 {@link AccessDeniedException} 을
 * 표준 403 봉투로 변환하고 보안 감사 로그({@code security.authz.denied})를 남긴다.
 * <p>이 예외는 필터가 아니라 MVC 핸들러 내부에서 발생하므로 {@code ExceptionTranslationFilter}(및 그 곳의
 * {@code LoggingAccessDeniedHandler})를 타지 않고 MVC 예외 처리로 흘러, 자칫 {@code GlobalExceptionHandler}의
 * 포괄 처리에 걸려 <b>500</b> 으로 잘못 응답될 수 있다(그리고 보안 로그도 누락). 더 구체적인 예외 타입
 * 핸들러가 우선하므로 이 어드바이스가 인가 거부를 403 으로 바로잡고 보안 이벤트를 보장한다.
 * <p>필터 단계의 인가 거부(경로 기반 룰)는 {@link LoggingAccessDeniedHandler} 가 담당한다(단일 요청에 둘 중
 * 하나만 동작 → 이중 로깅 없음). 보안 스타터가 있을 때만 등록된다(자동구성의 {@code @ConditionalOnClass}).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // GlobalExceptionHandler(Exception 포괄, 최저우선순위)보다 먼저 인가거부를 잡는다
public class SecurityExceptionAdvice {

    private final MessageResolver messages;
    private final SecurityAuditLogger securityAuditLogger;

    public SecurityExceptionAdvice(MessageResolver messages, SecurityAuditLogger securityAuditLogger) {
        this.messages = messages;
        this.securityAuditLogger = securityAuditLogger;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex,
                                                                HttpServletRequest request) {
        ErrorCode code = CommonErrorCode.FORBIDDEN;
        String detail = messages.get(code.messageKey());
        securityAuditLogger.authzDenied(request.getRequestURI(), request.getRemoteAddr(),
                currentPrincipal(), ex.getMessage());
        return ResponseEntity.status(code.status())
                .body(ApiResponse.error(code.code(), detail, ApiError.of(code.code(), detail)));
    }

    private static String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }
}
