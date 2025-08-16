package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.RiskMetric;
import com.example.memo.jpa.entity.purchase.analysis.Top5Sector;

public interface Top5SectorRepository extends JpaRepository<Top5Sector, Long>{
	List<Top5Sector> findByStatusIn(List<String> statuses);
}
