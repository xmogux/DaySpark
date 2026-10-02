package com.dayspark.app.repository;

import com.dayspark.app.model.FocusSession;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findByStatusIn(Collection<String> status);
    List<FocusSession> findByStatusOrderByStartedAtDesc(String status);
    List<FocusSession> findByCompletedDayBetweenAndStatus(LocalDate start, LocalDate end, String status);
    List<FocusSession> findByCompletedDayBetweenAndStatusIn(
        LocalDate start,
        LocalDate end,
        Collection<String> statuses
    );
    List<FocusSession> findByDareIdAndStatusIn(Long id, Collection<String> statuses);
    void deleteAllByDareId(Long id);
}
