package ai.mutuus.sample.board;

import ai.mutuus.common.api.PageResponse;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;

/**
 * 게시판 서비스 <b>계약</b> — 동일한 유스케이스를 JPA/jOOQ/Spring Data JDBC 세 방식으로 구현한다.
 * <p>모범사례 포인트: 컨트롤러는 이 계약(도메인 DTO)에만 의존하고 영속 기술을 모른다. 조회 실패는
 * {@link BoardErrorCode#POST_NOT_FOUND} 를 실은 {@code BusinessException} 으로 신호하며(→ 표준 404 봉투),
 * 목록은 라이브러리 {@link PageResponse} 로 페이징/검색 결과를 일관 반환한다.
 */
public interface BoardService {

    /** 게시글 생성 → 생성된 글. */
    BoardPostResponse create(BoardPostRequest request);

    /** 단건 조회 — 없으면 {@code BusinessException(POST_NOT_FOUND)}. */
    BoardPostResponse get(long id);

    /** 키워드(제목/작성자 부분일치, 공백이면 전체) 검색 + 페이징(0-based). */
    PageResponse<BoardPostResponse> search(String keyword, int page, int size);

    /** 수정 — 없으면 {@code BusinessException(POST_NOT_FOUND)}. */
    BoardPostResponse update(long id, BoardPostRequest request);

    /** 삭제 — 없으면 {@code BusinessException(POST_NOT_FOUND)}. */
    void delete(long id);

    /** 이 구현이 사용하는 데이터 접근 기술 라벨(JPA/jOOQ/JDBC). */
    String tech();
}
