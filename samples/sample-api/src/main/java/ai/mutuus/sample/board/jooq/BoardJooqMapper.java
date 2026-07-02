package ai.mutuus.sample.board.jooq;

import ai.mutuus.sample.board.dto.BoardPostResponse;
import org.mapstruct.Mapper;

/** jOOQ 행(POJO) → DTO 매핑(MapStruct). */
@Mapper(componentModel = "spring")
public interface BoardJooqMapper {

    BoardPostResponse toResponse(BoardPostRow row, String tech);
}
