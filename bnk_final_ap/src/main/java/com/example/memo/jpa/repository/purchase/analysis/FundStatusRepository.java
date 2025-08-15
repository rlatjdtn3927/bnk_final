package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.FundStatus;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface FundStatusRepository extends JpaRepository<FundStatus, Long> {
	List<FundStatus> findByStatusIn(List<String> statuses);
}
