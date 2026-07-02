package ai.mutuus.sample.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청(Record + Bean Validation). 비즈니스 규칙(작성자가 해당 글에 좋아요를 눌렀는지)은
 * 형식 검증이 아니라 <b>Service 계층</b>에서 확인한다({@code COMMENT_REQUIRES_LIKE}).
 */
public record CommentRequest(
        @NotBlank @Size(max = 50) String author,
        @NotBlank @Size(max = 1000) String content) {
}
