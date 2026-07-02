package ai.mutuus.sample.board.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 게시글 JPA 리포지토리. Spring Data JPA 강점(선언적 쿼리)을 보인다 — 페이징/검색은 {@link Query}(JPQL) +
 * {@link Pageable} 로, 단순 CRUD 는 {@link JpaRepository} 기본 메서드로.
 */
public interface BoardPostJpaRepository extends JpaRepository<BoardPost, Long> {

    /** 제목/작성자 부분일치(대소문자 무시) 검색 + 페이징. {@code kw} 가 null 이면 전체. */
    @Query("""
            select b from BoardPost b
            where :kw is null
               or lower(b.title)  like lower(concat('%', :kw, '%'))
               or lower(b.author) like lower(concat('%', :kw, '%'))
            """)
    Page<BoardPost> search(@Param("kw") String kw, Pageable pageable);
}
