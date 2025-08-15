package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.FundNav;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

public interface FundNavRepository extends JpaRepository<FundNav, Long> {
	List<FundNav> findByStatusIn(List<String> statuses);
}
