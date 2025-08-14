package com.example.memo.jpa.repository.purchase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.CumulativePerformance;


public interface CumulativePerformanceRepository extends JpaRepository<CumulativePerformance, Long> {
}
