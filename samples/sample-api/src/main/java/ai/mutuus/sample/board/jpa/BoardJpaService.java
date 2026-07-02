package ai.mutuus.sample.board.jpa;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 게시판 서비스 — <b>Spring Data JPA</b> 구현. 쓰기 트랜잭션 기본, 조회는 {@code readOnly}. 구문검증은 DTO 가,
 * <b>비즈니스 검증</b>(공지 규칙·중복 제목·좋아요 선행 댓글·대상 존재)은 이 계층이 담당한다.
 */
@Service
@Transactional
public class BoardJpaService implements BoardService {

    private final BoardPostJpaRepository posts;
    private final BoardLikeRepository likes;
    private final BoardCommentRepository comments;
    private final BoardJpaMapper mapper;

    public BoardJpaService(BoardPostJpaRepository posts, BoardLikeRepository likes,
                           BoardCommentRepository comments, BoardJpaMapper mapper) {
        this.posts = posts;
        this.likes = likes;
        this.comments = comments;
        this.mapper = mapper;
    }

    // ----- 게시글 -----

    @Override
    public BoardPostResponse create(BoardPostRequest request) {
        BoardRules.validateNotice(request.title(), request.author());          // 입력 상호관계
        if (posts.existsByAuthorAndTitle(request.author(), request.title())) {  // 기 데이터 사전검증
            throw new BusinessException(BoardErrorCode.DUPLICATE_TITLE);
        }
        BoardPost entity = mapper.toEntity(request);
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
        Page<BoardPost> found = posts.search(kw, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        List<BoardPostResponse> content = found.getContent().stream().map(mapper::toResponse).toList();
        return PageResponse.of(content, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    @Override
    public BoardPostResponse update(long id, BoardPostRequest request) {
        BoardPost entity = findPost(id);
        BoardRules.validateNotice(request.title(), request.author());
        if (posts.existsByAuthorAndTitleAndIdNot(request.author(), request.title(), id)) {
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
        posts.delete(findPost(id));
    }

    // ----- 좋아요 -----

    @Override
    public long like(long postId, String author) {
        ensurePostExists(postId);
        if (!likes.existsByPostIdAndAuthor(postId, author)) { // 멱등
            BoardLike like = new BoardLike();
            like.setPostId(postId);
            like.setAuthor(author);
            like.setCreatedAt(Instant.now());
            likes.save(like);
        }
        return likes.countByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public long likeCount(long postId) {
        ensurePostExists(postId);
        return likes.countByPostId(postId);
    }

    // ----- 댓글 -----

    @Override
    public CommentResponse addComment(long postId, CommentRequest request) {
        ensurePostExists(postId);
        // 핵심 비즈니스 규칙: 좋아요를 누른 사용자만 댓글 작성 가능
        if (!likes.existsByPostIdAndAuthor(postId, request.author())) {
            throw new BusinessException(BoardErrorCode.COMMENT_REQUIRES_LIKE);
        }
        BoardComment comment = new BoardComment();
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
        return comments.findByPostIdOrderByIdAsc(postId).stream().map(mapper::toCommentResponse).toList();
    }

    private BoardPost findPost(long id) {
        return posts.findById(id).orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
    }

    private void ensurePostExists(long id) {
        if (!posts.existsById(id)) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
    }
}
