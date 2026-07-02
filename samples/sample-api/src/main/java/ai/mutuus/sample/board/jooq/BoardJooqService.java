package ai.mutuus.sample.board.jooq;

import java.util.List;

import ai.mutuus.common.api.PageResponse;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.sample.board.BoardErrorCode;
import ai.mutuus.sample.board.BoardRules;
import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import ai.mutuus.sample.board.dto.CommentRequest;
import ai.mutuus.sample.board.dto.CommentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 게시판 서비스 — <b>jOOQ</b> 구현. DAO 의 타입세이프 SQL 을 유스케이스로 조합하고 비즈니스 검증을 얹는다.
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
        BoardRules.validateNotice(request.title(), request.author());
        if (dao.existsByAuthorAndTitle(request.author(), request.title())) {
            throw new BusinessException(BoardErrorCode.DUPLICATE_TITLE);
        }
        return mapper.toResponse(dao.insert(request.title(), request.content(), request.author()), tech());
    }

    @Override
    @Transactional(readOnly = true)
    public BoardPostResponse get(long id) {
        return mapper.toResponse(findPost(id), tech());
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
        if (!dao.postExists(id)) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
        BoardRules.validateNotice(request.title(), request.author());
        if (dao.existsByAuthorAndTitleExcept(request.author(), request.title(), id)) {
            throw new BusinessException(BoardErrorCode.DUPLICATE_TITLE);
        }
        dao.update(id, request.title(), request.content(), request.author());
        return mapper.toResponse(dao.findById(id), tech());
    }

    @Override
    public void delete(long id) {
        if (dao.delete(id) == 0) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
    }

    @Override
    public long like(long postId, String author) {
        ensurePostExists(postId);
        if (!dao.likeExists(postId, author)) {
            dao.insertLike(postId, author);
        }
        return dao.countLikes(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public long likeCount(long postId) {
        ensurePostExists(postId);
        return dao.countLikes(postId);
    }

    @Override
    public CommentResponse addComment(long postId, CommentRequest request) {
        ensurePostExists(postId);
        if (!dao.likeExists(postId, request.author())) {
            throw new BusinessException(BoardErrorCode.COMMENT_REQUIRES_LIKE);
        }
        return mapper.toCommentResponse(dao.insertComment(postId, request.author(), request.content()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> comments(long postId) {
        ensurePostExists(postId);
        return dao.findComments(postId).stream().map(mapper::toCommentResponse).toList();
    }

    @Override
    public String tech() {
        return "jOOQ";
    }

    private BoardPostRow findPost(long id) {
        BoardPostRow row = dao.findById(id);
        if (row == null) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
        return row;
    }

    private void ensurePostExists(long id) {
        if (!dao.postExists(id)) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
    }
}
