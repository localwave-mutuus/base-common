package ai.mutuus.sample.board.jdbc;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
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
               or lower(title)  like lower('%' || :kw || '%') escape '\\'
               or lower(author) like lower('%' || :kw || '%') escape '\\'
            order by id desc limit :size offset :off
            """)
    List<BoardPostJdbc> search(@Param("kw") String kw, @Param("size") int size, @Param("off") int off);

    @Query("""
            select count(*) from board_post
            where :kw is null
               or lower(title)  like lower('%' || :kw || '%') escape '\\'
               or lower(author) like lower('%' || :kw || '%') escape '\\'
            """)
    long countSearch(@Param("kw") String kw);

    @Query("select count(*) > 0 from board_post where author = :author and title = :title")
    boolean existsByAuthorAndTitle(@Param("author") String author, @Param("title") String title);

    @Query("select count(*) > 0 from board_post where author = :author and title = :title and id <> :id")
    boolean existsByAuthorAndTitleExcept(@Param("author") String author, @Param("title") String title, @Param("id") long id);

    /**
     * 단일 쿼리 삭제 — 존재확인({@code existsById}) + {@code deleteById}(2쿼리)를 1쿼리로 합친다.
     * 반영 행 수를 돌려주므로 서비스가 0건이면 {@code POST_NOT_FOUND} 로 신호할 수 있다(jOOQ DAO 와 동일 관용구).
     * 자식(좋아요/댓글)은 FK {@code on delete cascade}(V3 마이그레이션)로 DB 가 함께 정리한다.
     */
    @Modifying
    @Query("delete from board_post where id = :id")
    int deleteByIdReturningCount(@Param("id") long id);
}
