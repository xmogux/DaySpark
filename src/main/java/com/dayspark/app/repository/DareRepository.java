package com.dayspark.app.repository;

import com.dayspark.app.model.Dare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DareRepository extends JpaRepository<Dare, Long> {
    List<Dare> findAllByOrderByActiveDescIdAsc();
    List<Dare> findByActiveTrueOrderByIdAsc();
}
