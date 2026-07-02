package ai.mutuus.sample.board.web;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.api.PageResponse;
import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 게시판 REST 공통 컨트롤러(모범사례: <b>얇은 컨트롤러</b> — HTTP 매핑·검증·표준 봉투만 담당하고 영속 기술은 모른다).
 * 세 스택 컨트롤러가 이 클래스를 상속하고 {@link #service()} 만 각자의 {@link BoardService} 구현으로 제공한다.
 * <ul>
 *   <li>목록: {@code GET ?keyword=&page=&size=} → {@link PageResponse}</li>
 *   <li>단건: {@code GET /{id}} (없으면 서비스가 404 신호)</li>
 *   <li>생성/수정: {@code POST} / {@code PUT /{id}} (Bean Validation)</li>
 *   <li>삭제: {@code DELETE /{id}}</li>
 * </ul>
 * 모든 응답은 라이브러리 표준 {@link ApiResponse} 봉투로 나간다.
 */
public abstract class AbstractBoardController {

    /** 이 게시판이 사용할 데이터 접근 구현. */
    protected abstract BoardService service();

    @GetMapping
    public ApiResponse<PageResponse<BoardPostResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service().search(keyword, page, size));
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
}
