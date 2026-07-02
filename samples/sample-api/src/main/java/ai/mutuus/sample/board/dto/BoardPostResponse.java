package ai.mutuus.sample.board.dto;

import java.time.Instant;

/**
 * 게시글 응답 DTO — <b>Java Record</b>(불변). 세 스택 샘플 공통. {@code tech} 로 어느 스택이 처리했는지 표시.
 */
public record BoardPostResponse(
        Long id,
        String title,
        String content,
        String author,
        Instant createdAt,
        String tech) {
}
