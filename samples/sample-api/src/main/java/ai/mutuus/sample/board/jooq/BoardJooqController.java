package ai.mutuus.sample.board.jooq;

import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import ai.mutuus.common.api.PageResponse;
import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.dto.BoardSearchRequest;
import ai.mutuus.sample.board.web.AbstractBoardController;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게시판 — <b>jOOQ</b> 엔드포인트. 공통 CRUD 는 {@link AbstractBoardController}, 로직은 {@link BoardJooqService}. */
@RestController
@RequestMapping("/demo/board/jooq")
public class BoardJooqController extends AbstractBoardController {

    private final BoardJooqService service;

    public BoardJooqController(BoardJooqService service) {
        this.service = service;
    }

    @Override
    protected BoardService service() {
        return service;
    }

    /**
     * <b>동적 검색</b>(jOOQ 특화, 다른 스택엔 없음). 선택 조건(keyword LIKE·authors IN·createdFrom~To)만
     * WHERE 로 조립하고, {@code fields} 로 응답 컬럼을 고른다(동적 SELECT). 복잡 조건이라 POST 본문으로 받는다.
     */
    @Operation(summary = "게시글 동적 검색(jOOQ)",
            description = "값이 있는 조건만 WHERE 조립(LIKE/IN/from~to) + 동적 SELECT(fields, 허용목록). "
                    + "허용목록 밖 필드 400, 범위 역전 422.")
    @PostMapping("/search")
    public ApiResponse<PageResponse<Map<String, Object>>> search(@Valid @RequestBody BoardSearchRequest request) {
        return ApiResponse.ok(service.dynamicSearch(request));
    }
}
