package ai.mutuus.sample.board.jpa;

import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * JPA 엔티티 ↔ DTO 매핑 — <b>MapStruct</b>(컴파일 타임 생성, 필드 누락 시 컴파일 에러).
 */
@Mapper(componentModel = "spring")
public interface BoardJpaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    BoardPost toEntity(BoardPostRequest req);

    BoardPostResponse toResponse(BoardPost entity, String tech);
}
