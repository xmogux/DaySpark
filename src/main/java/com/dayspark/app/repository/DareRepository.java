package com.dayspark.app.repository;

import com.dayspark.app.model.Dare;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DareRepository extends JpaRepository<Dare, Long> {
    List<Dare> findAllByOrderByActiveDescIdAsc();
    List<Dare> findByActiveTrueOrderByIdAsc();
}
