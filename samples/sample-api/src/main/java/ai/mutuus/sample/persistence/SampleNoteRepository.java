package ai.mutuus.sample.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** {@link SampleNote} 데모 리포지토리. */
public interface SampleNoteRepository extends JpaRepository<SampleNote, Long> {
}
