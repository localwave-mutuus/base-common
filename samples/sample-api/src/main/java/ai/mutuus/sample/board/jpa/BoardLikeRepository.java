package ai.mutuus.sample.board.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    boolean existsByPostIdAndAuthor(Long postId, String author);

    long countByPostId(Long postId);
}
