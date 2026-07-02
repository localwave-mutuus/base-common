package ai.mutuus.sample.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 좋아요 요청(Record). 누른 사람 식별용 author. */
public record LikeRequest(@NotBlank @Size(max = 50) String author) {
}
