package com.dayspark.app.repository;
import com.dayspark.app.model.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException,Long>{
 Optional<ScheduleException> findByDareIdAndDay(Long id,LocalDate date);
 List<ScheduleException> findByDay(LocalDate date);
 List<ScheduleException> findByDareIdOrderByDayDesc(Long dareId);
 void deleteAllByDareId(Long id);
}
