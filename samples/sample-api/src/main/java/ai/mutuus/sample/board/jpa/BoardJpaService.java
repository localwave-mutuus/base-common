package ai.mutuus.sample.board.jpa;

import java.time.Instant;
import java.util.List;

import ai.mutuus.common.api.PageResponse;
import ai.mutuus.common.exception.BusinessException;
import ai.mutuus.sample.board.BoardErrorCode;
import ai.mutuus.sample.board.BoardService;
import ai.mutuus.sample.board.dto.BoardPostRequest;
import ai.mutuus.sample.board.dto.BoardPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 게시판 서비스 — <b>Spring Data JPA</b> 구현. 모범사례: 클래스는 쓰기 트랜잭션 기본,
 * 조회는 {@code @Transactional(readOnly=true)}(라이브러리 라우팅이 있으면 Replica 로 감). 조회 실패는
 * {@link BoardErrorCode#POST_NOT_FOUND} 로 신호(→ 표준 404). 매핑은 MapStruct, 페이징은 {@link PageResponse}.
 */
@Service
@Transactional
public class BoardJpaService implements BoardService {

    private final BoardPostJpaRepository repository;
    private final BoardJpaMapper mapper;

    public BoardJpaService(BoardPostJpaRepository repository, BoardJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public BoardPostResponse create(BoardPostRequest request) {
        BoardPost entity = mapper.toEntity(request);
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
        Page<BoardPost> found = repository.search(kw, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        List<BoardPostResponse> content = found.getContent().stream().map(e -> mapper.toResponse(e, tech())).toList();
        return PageResponse.of(content, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    @Override
    public BoardPostResponse update(long id, BoardPostRequest request) {
        BoardPost entity = find(id);
        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setAuthor(request.author());
        return mapper.toResponse(repository.save(entity), tech());
    }

    @Override
    public void delete(long id) {
        repository.delete(find(id));
    }

    @Override
    public String tech() {
        return "JPA";
    }

    private BoardPost find(long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
    }
}
