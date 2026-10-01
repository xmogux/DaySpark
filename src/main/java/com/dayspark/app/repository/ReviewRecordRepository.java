package com.dayspark.app.repository;

import com.dayspark.app.model.ReviewRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    Optional<ReviewRecord> findByDareIdAndOffsetDays(Long dareId, int offsetDays);
    List<ReviewRecord> findByDareIdOrderByOffsetDaysAsc(Long dareId);
    void deleteAllByDareId(Long dareId);
}
