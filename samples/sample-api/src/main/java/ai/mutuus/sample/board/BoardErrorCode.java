package ai.mutuus.sample.board;

import ai.mutuus.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 게시판 도메인 에러 코드 — 라이브러리 {@link ErrorCode} 인터페이스 구현(모범사례: 도메인 에러는 소비 서비스가
 * 자체 enum 으로). {@code BusinessException} 에 실으면 {@code GlobalExceptionHandler} 가 표준 {@code ApiResponse}
 * 봉투(상태·code·i18n 메시지·traceId)로 변환한다. 메시지는 {@code messages/sample-messages}.
 * <p>구문 검증(형식/범위/유무)은 Bean Validation(400)이 담당하고, <b>여기 코드들은 비즈니스(의미) 검증</b>
 * — 기존 데이터/입력 상호관계에 대한 규칙 위반을 표현한다.
 */
public enum BoardErrorCode implements ErrorCode {

    /** 게시글을 찾을 수 없음(404). */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "error.board.post.notfound"),

    /** 같은 작성자의 동일 제목이 이미 존재(409) — 기 데이터 사전검증. */
    DUPLICATE_TITLE(HttpStatus.CONFLICT, "error.board.title.duplicate"),

    /** 공지([공지] 제목)는 지정 작성자만 허용(403) — 입력 상호관계(제목↔작성자) 검증. */
    NOTICE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "error.board.notice.forbidden"),

    /** 좋아요를 누르지 않은 사용자의 댓글 작성 시도(422) — cross-entity 사전검증. */
    COMMENT_REQUIRES_LIKE(HttpStatus.UNPROCESSABLE_CONTENT, "error.board.comment.requireslike");

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
