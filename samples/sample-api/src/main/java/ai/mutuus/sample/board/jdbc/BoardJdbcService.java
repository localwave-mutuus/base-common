package ai.mutuus.sample.board.jdbc;

import java.time.Instant;
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
 * 게시판 서비스 — <b>Spring Data JDBC</b> 구현. CrudRepository + 명시 SQL(@Query) 로 예측 가능한 CRUD/페이징.
 * 조회 실패는 {@link BoardErrorCode#POST_NOT_FOUND}.
 */
@Service
@Transactional
public class BoardJdbcService implements BoardService {

    private final BoardPostJdbcRepository repository;
    private final BoardJdbcMapper mapper;

    public BoardJdbcService(BoardPostJdbcRepository repository, BoardJdbcMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public BoardPostResponse create(BoardPostRequest request) {
        BoardPostJdbc entity = mapper.toEntity(request);
        entity.setCreatedAt(Instant.now());
        return mapper.toResponse(repository.save(entity), tech());
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
        long total = repository.countSearch(kw);
        List<BoardPostResponse> content = repository.search(kw, size, page * size).stream()
                .map(e -> mapper.toResponse(e, tech())).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public BoardPostResponse update(long id, BoardPostRequest request) {
        BoardPostJdbc entity = find(id);
        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setAuthor(request.author());
        return mapper.toResponse(repository.save(entity), tech());
    }

    @Override
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new BusinessException(BoardErrorCode.POST_NOT_FOUND);
        }
        repository.deleteById(id);
    }

    @Override
    public String tech() {
        return "JDBC";
    }

    private BoardPostJdbc find(long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
    }
}
