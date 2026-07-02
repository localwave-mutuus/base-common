package ai.mutuus.sample.board.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA 리포지토리 — <b>메서드명 기반 선언적 쿼리</b>(Spring Data JPA 강점).
 */
public interface BoardPostJpaRepository extends JpaRepository<BoardPost, Long> {

    /** 작성자별 최신순 조회(메서드명 파생 쿼리). */
    List<BoardPost> findByAuthorOrderByIdDesc(String author);
}
