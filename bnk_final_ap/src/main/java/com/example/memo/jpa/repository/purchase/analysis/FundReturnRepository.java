package com.example.memo.jpa.repository.purchase.analysis;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.memo.jpa.entity.purchase.analysis.FundReturn;

public interface FundReturnRepository extends JpaRepository<FundReturn, Long> {
	List<FundReturn> findByStatusIn(List<String> statuses);
	List<FundReturn> findByReferenceDate(LocalDate referenceDate);
	Optional<FundReturn> findTopByOrderByReferenceDateDesc();
}
