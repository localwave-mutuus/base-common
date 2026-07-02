package ai.mutuus.sample.board.web;

import ai.mutuus.common.api.ApiError;
import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.exception.ErrorCode;
import ai.mutuus.common.i18n.MessageResolver;
import ai.mutuus.common.logging.AccessLogger;
import ai.mutuus.sample.board.BoardErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 게시판 컨트롤러 전용 예외 보정(모범사례: <b>인프라 예외를 도메인 의미로 번역</b>). 서비스가 명시적으로 던지는
 * {@code STALE_UPDATE}(클라이언트 {@code version} 불일치) 외에, JPA/JDBC 가 <b>flush/commit 시점</b>(서비스 메서드
 * 반환 후)에 던지는 {@link OptimisticLockingFailureException}(내가 읽은 뒤 남이 먼저 수정한 진짜 동시성 레이스)까지
 * 동일한 표준 봉투의 409(STALE_UPDATE) 로 매핑한다 — 라이브러리 {@code GlobalExceptionHandler} 의 500 폴백을
 * 도메인 의미로 앞질러 처리한다({@code basePackageClasses} 로 게시판 컨트롤러에만 적용).
 */
@RestControllerAdvice(basePackageClasses = AbstractBoardController.class)
public class BoardExceptionAdvice {

    private final MessageResolver messages;
    private final AccessLogger accessLogger;

    public BoardExceptionAdvice(MessageResolver messages, AccessLogger accessLogger) {
        this.messages = messages;
        this.accessLogger = accessLogger;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleStaleUpdate(OptimisticLockingFailureException ex,
                                                               HttpServletRequest request) {
        ErrorCode code = BoardErrorCode.STALE_UPDATE;
        String detail = messages.get(code.messageKey());
        accessLogger.businessError(code.code(), request.getRequestURI(), detail);
        return ResponseEntity.status(code.status())
                .body(ApiResponse.error(code.code(), detail, ApiError.of(code.code(), detail)));
    }
}
