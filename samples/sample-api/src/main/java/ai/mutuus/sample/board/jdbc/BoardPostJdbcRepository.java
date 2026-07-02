package ai.mutuus.sample.board.jdbc;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

/**
 * Spring Data JDBC 리포지토리 — {@link ListCrudRepository}(List 반환 CRUD). 파생 쿼리도 지원.
 */
public interface BoardPostJdbcRepository extends ListCrudRepository<BoardPostJdbc, Long> {

    List<BoardPostJdbc> findByAuthor(String author);
}
