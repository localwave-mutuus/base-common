package ai.mutuus.sample.board.jpa;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시판 CRUD — <b>Spring Data JPA(Hibernate)</b> 버전. Record DTO + MapStruct + Bean Validation 적용.
 * 메서드명 파생 쿼리({@code by-author})도 시연.
 */
@RestController
@RequestMapping("/demo/board/jpa")
public class BoardJpaController {

    private static final String TECH = "JPA";

    private final BoardPostJpaRepository repo;
    private final BoardJpaMapper mapper;

    public BoardJpaController(BoardPostJpaRepository repo, BoardJpaMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @PostMapping
    public ApiResponse<BoardPostResponse> create(@Valid @RequestBody BoardPostRequest req) {
        BoardPost entity = mapper.toEntity(req);
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
        return ApiResponse.ok(repo.findByAuthorOrderByIdDesc(author).stream()
                .map(e -> mapper.toResponse(e, TECH)).toList());
    }

    private ResponseEntity<?> notFound(long id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "[JPA] id=" + id + " 게시글 없음"));
    }
}
