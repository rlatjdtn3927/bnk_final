package com.example.memo.jpa.repository.purchase.analysis;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.analysis.FundNav;

public interface FundNavRepository extends JpaRepository<FundNav, Long> {
	List<FundNav> findByStatusIn(List<String> statuses);

	 /** 특정 펀드의 최신 NAV (reference_date DESC) */
    Optional<FundNav> findTopByFund_ProductIdOrderByReferenceDateDesc(String productId);
	}
