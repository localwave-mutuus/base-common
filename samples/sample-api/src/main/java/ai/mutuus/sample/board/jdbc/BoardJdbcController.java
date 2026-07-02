package ai.mutuus.sample.board.jdbc;

import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.web.AbstractBoardController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게시판 — <b>Spring Data JDBC</b> 엔드포인트. 로직은 {@link BoardJdbcService}. */
@RestController
@RequestMapping("/demo/board/jdbc")
public class BoardJdbcController extends AbstractBoardController {

    private final BoardJdbcService service;

    public BoardJdbcController(BoardJdbcService service) {
        this.service = service;
    }

    @Override
    protected BoardService service() {
        return service;
    }
}
