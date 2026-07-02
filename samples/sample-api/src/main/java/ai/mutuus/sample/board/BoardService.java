package ai.mutuus.sample.board;

import java.util.List;

import ai.mutuus.common.api.PageResponse;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import ai.mutuus.sample.board.dto.CommentRequest;
import ai.mutuus.sample.board.dto.CommentResponse;

/**
 * 게시판 서비스 <b>계약</b> — 동일 유스케이스를 JPA/jOOQ/Spring Data JDBC 세 방식으로 구현한다.
 * <p>모범사례 포인트:
 * <ul>
 *   <li><b>구문 검증</b>(형식/범위/유무)은 DTO {@code @Valid} 로(400), <b>비즈니스(의미) 검증</b>은 여기 서비스에서
 *       기존 데이터·입력 상호관계를 보고 {@code BusinessException(BoardErrorCode)} 로 신호한다.</li>
 *   <li>게시글 제목 중복(작성자+제목) → {@code DUPLICATE_TITLE}(409), 공지 규칙(제목↔작성자) → {@code NOTICE_NOT_ALLOWED}(403).</li>
 *   <li><b>댓글은 좋아요 선행 필수</b> — 좋아요를 누른 사용자만 댓글 작성 → {@code COMMENT_REQUIRES_LIKE}(422).</li>
 *   <li>없는 글 대상 조회/좋아요/댓글 → {@code POST_NOT_FOUND}(404). 좋아요 중복은 멱등.</li>
 * </ul>
 */
public interface BoardService {

    // ----- 게시글 -----
    BoardPostResponse create(BoardPostRequest request);

    BoardPostResponse get(long id);

    PageResponse<BoardPostResponse> search(String keyword, int page, int size);

    BoardPostResponse update(long id, BoardPostRequest request);

    void delete(long id);

    // ----- 좋아요 -----
    /** 게시글에 좋아요(멱등). 글이 없으면 {@code POST_NOT_FOUND}. @return 현재 좋아요 수. */
    long like(long postId, String author);

    /** 게시글 좋아요 수. */
    long likeCount(long postId);

    // ----- 댓글 -----
    /** 댓글 작성 — <b>author 가 해당 글에 좋아요를 눌렀어야 함</b>. 미선행 시 {@code COMMENT_REQUIRES_LIKE}. */
    CommentResponse addComment(long postId, CommentRequest request);

    /** 댓글 목록(오래된 순). 글이 없으면 {@code POST_NOT_FOUND}. */
    List<CommentResponse> comments(long postId);

    String tech();
}
