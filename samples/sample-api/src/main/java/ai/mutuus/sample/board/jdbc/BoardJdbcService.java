package ai.mutuus.sample.board.jdbc;

import java.time.Instant;
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
 * 게시판 서비스 — <b>Spring Data JDBC</b> 구현. 명시 SQL(@Query) 기반 CRUD/페이징 + 비즈니스 검증.
 */
@Service
@Transactional
public class BoardJdbcService implements BoardService {

    private final BoardPostJdbcRepository posts;
    private final BoardLikeJdbcRepository likes;
    private final BoardCommentJdbcRepository comments;
    private final BoardJdbcMapper mapper;

    public BoardJdbcService(BoardPostJdbcRepository posts, BoardLikeJdbcRepository likes,
                            BoardCommentJdbcRepository comments, BoardJdbcMapper mapper) {
        this.posts = posts;
        this.likes = likes;
        this.comments = comments;
        this.mapper = mapper;
    }

    @Override
    public BoardPostResponse create(BoardPostRequest request) {
        BoardRules.validateNotice(request.title(), request.author());
        if (posts.existsByAuthorAndTitle(request.author(), request.title())) {
            throw new BusinessException(BoardErrorCode.DUPLICATE_TITLE);
        }
        BoardPostJdbc entity = mapper.toEntity(request);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return mapper.toResponse(posts.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public BoardPostResponse get(long id) {
        return mapper.toResponse(findPost(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BoardPostResponse> search(String keyword, int page, int size) {
        String kw = StringUtils.hasText(keyword) ? BoardRules.escapeLike(keyword.trim()) : null;
        long total = posts.countSearch(kw);
        List<BoardPostResponse> content = posts.search(kw, size, page * size).stream()
                .map(mapper::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public BoardPostResponse update(long id, BoardPostRequest request, Long expectedVersion) {
        BoardPostJdbc entity = findPost(id);
        if (expectedVersion != null && !expectedVersion.equals(entity.getVersion())) {
            throw new BusinessException(BoardErrorCode.STALE_UPDATE);
        }
        BoardRules.validateNotice(request.title(), request.author());
        if (posts.existsByAuthorAndTitleExcept(request.author(), request.title(), id)) {
            throw new BusinessException(BoardErrorCode.DUPLICATE_TITLE);
        }
        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setAuthor(request.author());
        entity.setUpdatedAt(Instant.now());
        return mapper.toResponse(posts.save(entity));
    }

    @Override
    public void delete(long id) {
        if (posts.deleteByIdReturningCount(id) == 0) { // 단일 쿼리(존재확인+삭제). 자식은 FK cascade 로 정리
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
    }

    @Override
    public long like(long postId, String author) {
        ensurePostExists(postId);
        if (!likes.existsByPostAndAuthor(postId, author)) {
            BoardLikeJdbc like = new BoardLikeJdbc();
            like.setPostId(postId);
            like.setAuthor(author);
            like.setCreatedAt(Instant.now());
            likes.save(like);
        }
        return likes.countByPost(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public long likeCount(long postId) {
        ensurePostExists(postId);
        return likes.countByPost(postId);
    }

    @Override
    public CommentResponse addComment(long postId, CommentRequest request) {
        ensurePostExists(postId);
        if (!likes.existsByPostAndAuthor(postId, request.author())) {
            throw new BusinessException(BoardErrorCode.COMMENT_REQUIRES_LIKE);
        }
        BoardCommentJdbc comment = new BoardCommentJdbc();
        comment.setPostId(postId);
        comment.setAuthor(request.author());
        comment.setContent(request.content());
        comment.setCreatedAt(Instant.now());
        return mapper.toCommentResponse(comments.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> comments(long postId) {
        ensurePostExists(postId);
        return comments.findByPost(postId).stream().map(mapper::toCommentResponse).toList();
    }

    private BoardPostJdbc findPost(long id) {
        return posts.findById(id).orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
    }

    private void ensurePostExists(long id) {
        if (!posts.existsById(id)) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
    }
}
