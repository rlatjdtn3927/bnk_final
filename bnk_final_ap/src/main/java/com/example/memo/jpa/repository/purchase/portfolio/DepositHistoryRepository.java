// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/DepositHistoryRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.portfolio.DepositHistory;
//입금내역 리포지토리
public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
	/** 계좌별 입금내역(최신순) */
	List<DepositHistory> findByAccountTypeAndAcountIdOrderByDepositDateDesc(
            AccountType accountType, String acountId);

    /** 특정 기간의 입금내역 */
	List<DepositHistory> findByAccountTypeAndAcountIdAndDepositDateBetween(
            AccountType accountType, String acountId, LocalDateTime from, LocalDateTime to);
}
