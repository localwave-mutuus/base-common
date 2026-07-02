package ai.mutuus.sample.board.jpa;

import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.web.AbstractBoardController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게시판 — <b>Spring Data JPA(Hibernate)</b> 엔드포인트. 로직은 {@link BoardJpaService}. */
@RestController
@RequestMapping("/demo/board/jpa")
public class BoardJpaController extends AbstractBoardController {

    private final BoardJpaService service;

    public BoardJpaController(BoardJpaService service) {
        this.service = service;
    }

    @Override
    protected BoardService service() {
        return service;
    }
}
