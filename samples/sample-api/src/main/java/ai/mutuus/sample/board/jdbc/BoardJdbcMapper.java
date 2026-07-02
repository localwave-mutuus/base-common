package ai.mutuus.sample.board.jdbc;

import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import ai.mutuus.sample.board.dto.CommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Spring Data JDBC 애그리거트 ↔ DTO 매핑(MapStruct). */
@Mapper(componentModel = "spring")
public interface BoardJdbcMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true) // @Version — Spring Data JDBC 가 관리
    BoardPostJdbc toEntity(BoardPostRequest req);

    BoardPostResponse toResponse(BoardPostJdbc entity);

    CommentResponse toCommentResponse(BoardCommentJdbc comment);
}
