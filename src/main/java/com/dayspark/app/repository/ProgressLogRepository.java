package com.dayspark.app.repository;

import com.dayspark.app.model.ProgressLog;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {
    List<ProgressLog> findByDareIdAndDay(Long dareId, LocalDate day);
    void deleteAllByDareId(Long id);
}
