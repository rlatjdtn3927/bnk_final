package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.FundReturn;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface FundReturnRepository extends JpaRepository<FundReturn, Long> {
	List<FundReturn> findByStatusIn(List<String> statuses);
}
