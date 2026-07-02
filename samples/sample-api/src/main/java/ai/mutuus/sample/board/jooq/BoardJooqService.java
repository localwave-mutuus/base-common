package ai.mutuus.sample.board.jooq;

import java.util.List;

import ai.mutuus.common.api.PageResponse;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.sample.board.BoardErrorCode;
import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 게시판 서비스 — <b>jOOQ</b> 구현. DAO 의 타입세이프 SQL 을 유스케이스로 조합하고, 페이징은 count + limit/offset
 * 결과를 {@link PageResponse} 로 담는다. 조회 실패는 {@link BoardErrorCode#POST_NOT_FOUND}.
 */
@Service
@Transactional
public class BoardJooqService implements BoardService {

    private final BoardJooqDao dao;
    private final BoardJooqMapper mapper;

    public BoardJooqService(BoardJooqDao dao, BoardJooqMapper mapper) {
        this.dao = dao;
        this.mapper = mapper;
    }

    @Override
    public BoardPostResponse create(BoardPostRequest request) {
        return mapper.toResponse(dao.insert(request.title(), request.content(), request.author()), tech());
    }

    @Override
    @Transactional(readOnly = true)
    public BoardPostResponse get(long id) {
        return mapper.toResponse(find(id), tech());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BoardPostResponse> search(String keyword, int page, int size) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        long total = dao.count(kw);
        List<BoardPostResponse> content = dao.search(kw, size, page * size).stream()
                .map(r -> mapper.toResponse(r, tech())).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public BoardPostResponse update(long id, BoardPostRequest request) {
        if (dao.update(id, request.title(), request.content(), request.author()) == 0) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
        return mapper.toResponse(dao.findById(id), tech());
    }

    @Override
    public void delete(long id) {
        if (dao.delete(id) == 0) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
    }

    @Override
    public String tech() {
        return "jOOQ";
    }

    private BoardPostRow find(long id) {
        BoardPostRow row = dao.findById(id);
        if (row == null) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
        return row;
    }
}
