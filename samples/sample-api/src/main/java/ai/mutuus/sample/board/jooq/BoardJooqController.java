package ai.mutuus.sample.board.jooq;

import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.web.AbstractBoardController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게시판 — <b>jOOQ</b> 엔드포인트. 로직은 {@link BoardJooqService}. */
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
}
