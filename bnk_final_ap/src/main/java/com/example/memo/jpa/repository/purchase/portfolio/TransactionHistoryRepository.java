// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/TransactionHistoryRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.TxnType;
import com.example.memo.jpa.entity.purchase.portfolio.TransactionHistory;

/**
 * FLOW: 거래내역 조회 전용 레포지토리
 * - 보유/거래내역 화면: 계좌별 전체 거래 최신순
 * - 만기 전환: 예금(BUY) 납입건 조회, 기간/월별 묶음
 */
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    // [보유/거래내역 화면] 계좌별 전체 거래 최신순 (✅ 기존 서비스들에서 사용하던 메서드 복구/유지)
    List<TransactionHistory> findByAccountTypeAndAcountIdOrderByTxnDateDesc(
            AccountType accountType, String acountId);

    // [만기 전환] 특정 예금상품의 BUY 납입건 (전환일 이하)
    List<TransactionHistory> findByAccountTypeAndAcountIdAndTxnTypeAndProductIdAndTxnDateLessThanEqual(
            AccountType accountType, String acountId, TxnType txnType, String productId, LocalDate txnDate);

    // [만기 전환/월별 목록] 여러 예금상품들의 BUY 납입건 (기간)
    List<TransactionHistory> findByAccountTypeAndAcountIdAndTxnTypeAndProductIdInAndTxnDateBetween(
            AccountType accountType, String acountId, TxnType txnType, List<String> productIds,
            LocalDate from, LocalDate to);
}
