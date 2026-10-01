package com.dayspark.app.repository;
import com.dayspark.app.model.ProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface ProgressLogRepository extends JpaRepository<ProgressLog,Long>{
 List<ProgressLog> findByDareIdAndDay(Long dareId,LocalDate day);
 List<ProgressLog> findByDareIdAndDayBetween(Long dareId,LocalDate start,LocalDate end);
 void deleteAllByDareId(Long id);
}
