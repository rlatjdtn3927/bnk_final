// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/PortfolioRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;
import com.example.memo.jpa.entity.purchase.portfolio.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    // [보유현황/미리보기 등] 계좌 전체 보유 조회
    List<Portfolio> findByAccountTypeAndAcountId(AccountType accountType, String acountId);

    // [전환/매수] 특정 상품 보유 조회(upsert용)
    Optional<Portfolio> findByAccountTypeAndAcountIdAndProductTypeAndProductId(
            AccountType accountType, String acountId, ProductType productType, String productId);

    // (신규) [만기예정 목록] 원리금보장 보유만 조회
    List<Portfolio> findByAccountTypeAndAcountIdAndProductType(
            AccountType accountType, String acountId, ProductType productType);
}
