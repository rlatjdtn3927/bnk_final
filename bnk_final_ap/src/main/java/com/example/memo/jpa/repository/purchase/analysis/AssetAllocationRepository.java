package com.example.memo.jpa.repository.purchase.analysis;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.AssetAllocation;



public interface AssetAllocationRepository extends JpaRepository<AssetAllocation, Long>{
	List<AssetAllocation> findByStatusIn(List<String> statuses);
	List<AssetAllocation> findByReferenceDate(LocalDate referenceDate);
	Optional<AssetAllocation> findTopByOrderByReferenceDateDesc();
}
