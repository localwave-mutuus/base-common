package ai.mutuus.sample.board.jooq;

import java.util.List;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시판 CRUD — <b>jOOQ</b> 버전. Record DTO + MapStruct + Bean Validation 적용.
 */
@RestController
@RequestMapping("/demo/board/jooq")
public class BoardJooqController {

    private static final String TECH = "jOOQ";

    private final BoardJooqDao dao;
    private final BoardJooqMapper mapper;

    public BoardJooqController(BoardJooqDao dao, BoardJooqMapper mapper) {
        this.dao = dao;
        this.mapper = mapper;
    }

    @PostMapping
    public ApiResponse<BoardPostResponse> create(@Valid @RequestBody BoardPostRequest req) {
        BoardPostRow row = dao.insert(req.title(), req.content(), req.author());
        return ApiResponse.ok(mapper.toResponse(row, TECH));
    }

    @GetMapping
    public ApiResponse<List<BoardPostResponse>> list() {
        return ApiResponse.ok(dao.findAll().stream().map(r -> mapper.toResponse(r, TECH)).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        BoardPostRow row = dao.findById(id);
        if (row == null) {
            return notFound(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(row, TECH)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @Valid @RequestBody BoardPostRequest req) {
        if (dao.update(id, req.title(), req.content(), req.author()) == 0) {
            return notFound(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(dao.findById(id), TECH)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (dao.delete(id) == 0) {
            return notFound(id);
        }
        return ResponseEntity.ok(ApiResponse.ok("deleted id=" + id));
    }

    @GetMapping("/by-author")
    public ApiResponse<List<BoardPostResponse>> byAuthor(@RequestParam String author) {
        return ApiResponse.ok(dao.findByAuthor(author).stream().map(r -> mapper.toResponse(r, TECH)).toList());
    }

    private ResponseEntity<?> notFound(long id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "[jOOQ] id=" + id + " 게시글 없음"));
    }
}
