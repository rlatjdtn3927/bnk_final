package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.AssetAllocation;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface AssetAllocationRepository extends JpaRepository<AssetAllocation, Long>{
	List<AssetAllocation> findByStatusIn(List<String> statuses);
}
