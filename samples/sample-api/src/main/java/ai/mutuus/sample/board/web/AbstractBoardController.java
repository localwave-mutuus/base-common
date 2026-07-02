package ai.mutuus.sample.board.web;

import java.util.List;
import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.api.PageResponse;
import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import ai.mutuus.sample.board.dto.CommentRequest;
import ai.mutuus.sample.board.dto.CommentResponse;
import ai.mutuus.sample.board.dto.LikeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 게시판 REST 공통 컨트롤러(모범사례: <b>얇은 컨트롤러</b> — HTTP 매핑·구문검증·표준봉투·파라미터 정규화만 담당,
 * 영속 기술과 비즈니스 규칙은 모른다). 세 스택 컨트롤러가 상속하고 {@link #service()} 만 각자 구현으로 제공한다.
 * <ul>
 *   <li>목록/단건/생성/수정/삭제 — 목록은 {@link PageResponse}(page/size 는 안전 범위로 정규화)</li>
 *   <li>좋아요: {@code POST /{id}/likes} · 댓글: {@code GET|POST /{id}/comments}</li>
 * </ul>
 * 비즈니스 검증(제목 중복·공지 규칙·댓글의 좋아요 선행 등)은 {@link BoardService} 가 수행하고 실패를 표준 봉투로 낸다.
 */
public abstract class AbstractBoardController {

    /** 페이지 크기 상한(과대 요청 자원 보호). */
    private static final int MAX_PAGE_SIZE = 100;

    protected abstract BoardService service();

    // ----- 게시글 -----

    @GetMapping
    public ApiResponse<PageResponse<BoardPostResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE); // 1..100 로 클램프
        return ApiResponse.ok(service().search(keyword, safePage, safeSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<BoardPostResponse> get(@PathVariable long id) {
        return ApiResponse.ok(service().get(id));
    }

    @PostMapping
    public ApiResponse<BoardPostResponse> create(@Valid @RequestBody BoardPostRequest request) {
        return ApiResponse.ok(service().create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BoardPostResponse> update(@PathVariable long id, @Valid @RequestBody BoardPostRequest request) {
        return ApiResponse.ok(service().update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable long id) {
        service().delete(id);
        return ApiResponse.ok("deleted id=" + id);
    }

    // ----- 좋아요 -----

    @PostMapping("/{id}/likes")
    public ApiResponse<Map<String, Object>> like(@PathVariable long id, @Valid @RequestBody LikeRequest request) {
        long count = service().like(id, request.author());
        return ApiResponse.ok(Map.of("postId", id, "likeCount", count));
    }

    // ----- 댓글 -----

    @GetMapping("/{id}/comments")
    public ApiResponse<List<CommentResponse>> comments(@PathVariable long id) {
        return ApiResponse.ok(service().comments(id));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<CommentResponse> addComment(@PathVariable long id, @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(service().addComment(id, request));
    }
}
