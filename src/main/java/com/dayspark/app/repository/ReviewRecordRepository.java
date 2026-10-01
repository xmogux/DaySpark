package com.dayspark.app.repository;

import com.dayspark.app.model.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    Optional<ReviewRecord> findByDareIdAndOffsetDays(Long dareId, int offsetDays);
    List<ReviewRecord> findByDareIdOrderByOffsetDaysAsc(Long dareId);
    void deleteAllByDareId(Long dareId);
}
