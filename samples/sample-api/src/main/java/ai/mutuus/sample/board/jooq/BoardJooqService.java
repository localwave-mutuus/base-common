package ai.mutuus.sample.board.jooq;

import java.util.List;
import java.util.Map;

import ai.mutuus.common.api.PageResponse;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.sample.board.BoardErrorCode;
import ai.mutuus.sample.board.BoardRules;
import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import ai.mutuus.sample.board.dto.BoardSearchRequest;
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
        return mapper.toResponse(dao.insert(request.title(), request.content(), request.author()));
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
        long total = dao.count(kw);
        List<BoardPostResponse> content = dao.search(kw, size, page * size).stream()
                .map(mapper::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }

    /** 페이지 크기 상한(과대 요청 자원 보호) — 컨트롤러 목록 API 와 동일 규약. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * <b>동적 검색</b>(jOOQ 특화) — 값이 있는 조건만 WHERE 로 조립하고, 요청한 컬럼만 SELECT 한다.
     * 형식 검증은 DTO 가, 여기서는 <b>의미 검증</b>(범위 역전·허용되지 않은 필드)과 정규화(키워드 이스케이프·
     * 작성자 공백 제거·페이징 클램프)를 담당한 뒤 DAO 에 위임한다.
     */
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> dynamicSearch(BoardSearchRequest req) {
        // 의미 검증 1: 작성일시 범위 역전(from > to) → 422
        if (req.createdFrom() != null && req.createdTo() != null && req.createdFrom().isAfter(req.createdTo())) {
            throw new BusinessException(BoardErrorCode.INVALID_SEARCH_RANGE);
        }
        // 의미 검증 2: 동적 SELECT 필드는 허용목록(allowlist) 안에서만 → 밖이면 400(임의 컬럼 주입 차단)
        List<String> fields = req.fields();
        if (fields != null) {
            for (String f : fields) {
                if (!BoardJooqDao.ALLOWED_FIELDS.contains(f)) {
                    throw new BusinessException(BoardErrorCode.INVALID_SEARCH_FIELD);
                }
            }
        }
        // 정규화: 키워드 이스케이프, 작성자 공백/빈값 제거, 페이징 안전 범위 클램프
        String kw = StringUtils.hasText(req.keyword()) ? BoardRules.escapeLike(req.keyword().trim()) : null;
        List<String> authors = (req.authors() == null) ? null
                : req.authors().stream().filter(StringUtils::hasText).map(String::trim).toList();
        if (authors != null && authors.isEmpty()) {
            authors = null; // 모두 공백이면 IN 미적용
        }
        int page = Math.max(req.page() == null ? 0 : req.page(), 0);
        int size = Math.min(Math.max(req.size() == null ? 10 : req.size(), 1), MAX_PAGE_SIZE);

        long total = dao.countDynamic(kw, authors, req.createdFrom(), req.createdTo());
        List<Map<String, Object>> content = dao.dynamicSearch(kw, authors,
                req.createdFrom(), req.createdTo(), fields, size, page * size);
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public BoardPostResponse update(long id, BoardPostRequest request, Long expectedVersion) {
        if (!dao.postExists(id)) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
        BoardRules.validateNotice(request.title(), request.author());
        if (dao.existsByAuthorAndTitleExcept(request.author(), request.title(), id)) {
            throw new BusinessException(BoardErrorCode.DUPLICATE_TITLE);
        }
        // 원자적 낙관적 락: 조건부 UPDATE 가 0건이면(존재는 확인됨) 버전 불일치 → STALE_UPDATE
        int affected = dao.update(id, request.title(), request.content(), request.author(), expectedVersion);
        if (expectedVersion != null && affected == 0) {
            throw new BusinessException(BoardErrorCode.STALE_UPDATE);
        }
        return mapper.toResponse(dao.findById(id));
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
