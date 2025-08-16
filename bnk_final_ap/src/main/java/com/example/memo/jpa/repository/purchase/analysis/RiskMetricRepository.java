package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.RiskMetric;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface RiskMetricRepository extends JpaRepository<RiskMetric, Long>{
	List<RiskMetric> findByStatusIn(List<String> statuses);
}
