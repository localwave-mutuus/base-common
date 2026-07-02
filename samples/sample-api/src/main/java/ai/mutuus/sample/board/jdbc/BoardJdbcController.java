package ai.mutuus.sample.board.jdbc;

import java.time.Instant;
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
 * 게시판 CRUD — <b>Spring Data JDBC</b> 버전. Record DTO + MapStruct + Bean Validation 적용.
 */
@RestController
@RequestMapping("/demo/board/jdbc")
public class BoardJdbcController {

    private static final String TECH = "JDBC";

    private final BoardPostJdbcRepository repo;
    private final BoardJdbcMapper mapper;

    public BoardJdbcController(BoardPostJdbcRepository repo, BoardJdbcMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @PostMapping
    public ApiResponse<BoardPostResponse> create(@Valid @RequestBody BoardPostRequest req) {
        BoardPostJdbc entity = mapper.toEntity(req);
        entity.setCreatedAt(Instant.now());
        return ApiResponse.ok(mapper.toResponse(repo.save(entity), TECH));
    }

    @GetMapping
    public ApiResponse<List<BoardPostResponse>> list() {
        return ApiResponse.ok(repo.findAll().stream().map(e -> mapper.toResponse(e, TECH)).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        return repo.findById(id)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(e, TECH))))
                .orElseGet(() -> notFound(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @Valid @RequestBody BoardPostRequest req) {
        return repo.findById(id).<ResponseEntity<?>>map(e -> {
            e.setTitle(req.title());
            e.setContent(req.content());
            e.setAuthor(req.author());
            return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(repo.save(e), TECH)));
        }).orElseGet(() -> notFound(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!repo.existsById(id)) {
            return notFound(id);
        }
        repo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("deleted id=" + id));
    }

    @GetMapping("/by-author")
    public ApiResponse<List<BoardPostResponse>> byAuthor(@RequestParam String author) {
        return ApiResponse.ok(repo.findByAuthor(author).stream().map(e -> mapper.toResponse(e, TECH)).toList());
    }

    private ResponseEntity<?> notFound(long id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "[JDBC] id=" + id + " 게시글 없음"));
    }
}
