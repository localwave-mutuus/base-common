package ai.mutuus.sample.board.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    List<BoardComment> findByPostIdOrderByIdAsc(Long postId);
}
