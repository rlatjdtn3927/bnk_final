package com.example.memo.jpa.repository.purchase.analysis;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.memo.jpa.entity.purchase.analysis.CumulativePerformance;

public interface CumulativePerformanceRepository extends JpaRepository<CumulativePerformance, Long> {
	List<CumulativePerformance> findByStatusIn(List<String> statuses);
	List<CumulativePerformance> findByReferenceDate(LocalDate referenceDate);
	Optional<CumulativePerformance> findTopByOrderByReferenceDateDesc();
}
