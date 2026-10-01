package com.dayspark.app.repository;

import com.dayspark.app.model.FocusSession;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    List<FocusSession> findByStatusIn(Collection<String> status);
    List<FocusSession> findByStatusOrderByStartedAtDesc(String status);
    List<FocusSession> findByCompletedDayBetweenAndStatus(LocalDate start, LocalDate end, String status);
    List<FocusSession> findByDareIdAndStatus(Long id, String status);
    void deleteAllByDareId(Long id);
}
