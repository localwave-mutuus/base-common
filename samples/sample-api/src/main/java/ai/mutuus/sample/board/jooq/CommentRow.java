package ai.mutuus.sample.board.jooq;

import java.time.Instant;

/** jOOQ 댓글 조회 행(POJO, Record) — MapStruct 로 CommentResponse 에 매핑. */
public record CommentRow(Long id, Long postId, String author, String content, Instant createdAt) {
}
