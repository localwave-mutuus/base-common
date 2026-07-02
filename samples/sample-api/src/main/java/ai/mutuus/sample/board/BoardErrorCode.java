package ai.mutuus.sample.board;

import ai.mutuus.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 게시판 도메인 에러 코드 — 라이브러리 {@link ErrorCode} 인터페이스를 구현한다(모범사례: 도메인별 에러 코드는
 * 소비 서비스가 자체 enum 으로 정의). {@code BusinessException} 에 실으면 {@code GlobalExceptionHandler} 가
 * 표준 {@code ApiResponse} 봉투(상태·code·i18n 메시지·traceId)로 변환한다. 메시지는 {@code messages/sample-messages}.
 */
public enum BoardErrorCode implements ErrorCode {

    /** 게시글을 찾을 수 없음(404). */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "error.board.post.notfound");

    private final HttpStatus status;
    private final String messageKey;

    BoardErrorCode(HttpStatus status, String messageKey) {
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
