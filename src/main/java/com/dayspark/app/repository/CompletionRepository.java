package com.dayspark.app.repository;

import com.dayspark.app.model.Completion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletionRepository extends JpaRepository<Completion, Long> {
    Optional<Completion> findByDareIdAndChallengeDate(Long dareId, LocalDate challengeDate);
    List<Completion> findByChallengeDate(LocalDate challengeDate);
    List<Completion> findByChallengeDateBetweenOrderByChallengeDateAsc(LocalDate start, LocalDate end);
    List<Completion> findAllByOrderByChallengeDateDescCompletedAtDesc();
    long countByChallengeDateBetween(LocalDate start, LocalDate end);
    void deleteAllByDare_Id(Long dareId);
}
