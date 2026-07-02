package ai.mutuus.sample.board.jdbc;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface BoardCommentJdbcRepository extends ListCrudRepository<BoardCommentJdbc, Long> {

    @Query("select * from board_comment where post_id = :postId order by id asc")
    List<BoardCommentJdbc> findByPost(@Param("postId") long postId);
}
