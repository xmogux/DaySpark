package com.dayspark.app.repository;

import com.dayspark.app.model.Dare;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface DareRepository extends JpaRepository<Dare, Long> {
    /** A stable shared row lock serializes active-session creation across concurrent HTTP requests. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Dare> findFirstByOrderByIdAsc();
    List<Dare> findAllByOrderByActiveDescIdAsc();
    List<Dare> findByActiveTrueOrderByIdAsc();
}
