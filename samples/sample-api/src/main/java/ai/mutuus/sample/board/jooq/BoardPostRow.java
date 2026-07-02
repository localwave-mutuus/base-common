package ai.mutuus.sample.board.jooq;

import java.time.Instant;

/** jOOQ 조회 결과 행(도메인 POJO, Record). MapStruct 로 DTO 에 매핑된다. */
public record BoardPostRow(Long id, String title, String content, String author,
                           Instant createdAt, Instant updatedAt) {
}
