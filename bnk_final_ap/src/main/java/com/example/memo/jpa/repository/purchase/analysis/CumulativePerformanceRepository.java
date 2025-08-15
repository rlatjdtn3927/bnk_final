package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.CumulativePerformance;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;


public interface CumulativePerformanceRepository extends JpaRepository<CumulativePerformance, Long> {
	List<CumulativePerformance> findByStatusIn(List<String> statuses);
}
