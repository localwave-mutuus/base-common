package ai.mutuus.sample.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 생성/수정 요청 DTO — <b>Java Record</b>(보일러플레이트 0, 불변) + <b>Bean Validation</b>(표준 어노테이션).
 * JPA/jOOQ/Spring Data JDBC 세 스택 샘플이 공통으로 사용한다.
 */
public record BoardPostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @NotBlank @Size(max = 50) String author) {
}
