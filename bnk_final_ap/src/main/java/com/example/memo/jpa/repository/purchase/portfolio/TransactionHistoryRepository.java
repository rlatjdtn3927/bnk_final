// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/TransactionHistoryRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.portfolio.TransactionHistory;
//거래내역 리포지토리
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
	/** 계좌별 거래내역(최신순) */
	 List<TransactionHistory> findByAccountTypeAndAcountIdOrderByTxnDateDesc(
	            AccountType accountType, String acountId);
	}

