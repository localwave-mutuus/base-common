package ai.mutuus.sample.board.jdbc;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JDBC 리포지토리 — {@link ListCrudRepository}(단순 CRUD) + {@link Query}(명시 SQL) 로 페이징/검색.
 * "마법" 최소 · 예측 가능한 SQL. (JPA 처럼 JPQL 이 아니라 실제 SQL 을 쓴다)
 */
public interface BoardPostJdbcRepository extends ListCrudRepository<BoardPostJdbc, Long> {

    @Query("""
            select * from board_post
            where :kw is null
               or lower(title)  like lower('%' || :kw || '%')
               or lower(author) like lower('%' || :kw || '%')
            order by id desc limit :size offset :off
            """)
    List<BoardPostJdbc> search(@Param("kw") String kw, @Param("size") int size, @Param("off") int off);

    @Query("""
            select count(*) from board_post
            where :kw is null
               or lower(title)  like lower('%' || :kw || '%')
               or lower(author) like lower('%' || :kw || '%')
            """)
    long countSearch(@Param("kw") String kw);

    @Query("select count(*) > 0 from board_post where author = :author and title = :title")
    boolean existsByAuthorAndTitle(@Param("author") String author, @Param("title") String title);

    @Query("select count(*) > 0 from board_post where author = :author and title = :title and id <> :id")
    boolean existsByAuthorAndTitleExcept(@Param("author") String author, @Param("title") String title, @Param("id") long id);
}
