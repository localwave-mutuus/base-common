package ai.mutuus.sample.board.dto;

import java.time.Instant;

/**
 * 게시글 응답 DTO — <b>Java Record</b>(불변, 순수 도메인). 어느 스택이 처리했는지 같은 데모 메타는 담지 않는다
 * (도메인 순수성 — 스택 구분은 호출 경로 {@code /demo/board/{jpa|jooq|jdbc}} 로 이미 드러난다).
 */
public record BoardPostResponse(
        Long id,
        String title,
        String content,
        String author,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
