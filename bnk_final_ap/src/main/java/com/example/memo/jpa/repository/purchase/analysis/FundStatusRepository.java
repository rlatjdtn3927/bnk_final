package com.example.memo.jpa.repository.purchase.analysis;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.memo.jpa.entity.purchase.analysis.FundStatus;

public interface FundStatusRepository extends JpaRepository<FundStatus, Long> {
	List<FundStatus> findByReferenceDate(LocalDate referenceDate);
	Optional<FundStatus> findTopByOrderByReferenceDateDesc();
}
