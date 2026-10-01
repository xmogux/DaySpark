package com.dayspark.app.repository;

import com.dayspark.app.model.ScheduleException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {
    Optional<ScheduleException> findByDareIdAndDay(Long id, LocalDate date);
    List<ScheduleException> findByDareIdOrderByDayDesc(Long dareId);
    void deleteAllByDareId(Long id);
}
