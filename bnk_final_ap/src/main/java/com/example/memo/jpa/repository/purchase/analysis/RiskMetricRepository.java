package com.example.memo.jpa.repository.purchase.analysis;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.memo.jpa.entity.purchase.analysis.RiskMetric;


public interface RiskMetricRepository extends JpaRepository<RiskMetric, Long>{
	List<RiskMetric> findByStatusIn(List<String> statuses);
	List<RiskMetric> findByReferenceDate(LocalDate referenceDate);
	Optional<RiskMetric> findTopByOrderByReferenceDateDesc();
}
