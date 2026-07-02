package ai.mutuus.sample.board.jdbc;

import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Spring Data JDBC 애그리거트 ↔ DTO 매핑(MapStruct). */
@Mapper(componentModel = "spring")
public interface BoardJdbcMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    BoardPostJdbc toEntity(BoardPostRequest req);

    BoardPostResponse toResponse(BoardPostJdbc entity, String tech);
}
