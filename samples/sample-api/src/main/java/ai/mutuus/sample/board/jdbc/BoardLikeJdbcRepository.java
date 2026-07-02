package ai.mutuus.sample.board.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface BoardLikeJdbcRepository extends ListCrudRepository<BoardLikeJdbc, Long> {

    @Query("select count(*) > 0 from board_like where post_id = :postId and author = :author")
    boolean existsByPostAndAuthor(@Param("postId") long postId, @Param("author") String author);

    @Query("select count(*) from board_like where post_id = :postId")
    long countByPost(@Param("postId") long postId);
}
