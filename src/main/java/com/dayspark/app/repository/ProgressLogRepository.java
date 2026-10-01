package com.dayspark.app.repository;

import com.dayspark.app.model.ProgressLog;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {
    List<ProgressLog> findByDareIdAndDay(Long dareId, LocalDate day);
    List<ProgressLog> findByDareIdAndDayBetween(Long dareId, LocalDate start, LocalDate end);
    void deleteAllByDareId(Long id);
}
