package ai.mutuus.sample.board.dto;

import java.time.Instant;

/** 댓글 응답(Record). */
public record CommentResponse(Long id, Long postId, String author, String content, Instant createdAt) {
}
