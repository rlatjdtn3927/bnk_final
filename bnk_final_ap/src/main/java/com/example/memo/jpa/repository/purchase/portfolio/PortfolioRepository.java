// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/PortfolioRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;
import com.example.memo.jpa.entity.purchase.portfolio.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    // FLOW: 보유현황/미리보기용
    List<Portfolio> findByAccountTypeAndAcountId(AccountType accountType, String acountId);

    // FLOW: 만기 목록 조회(PRINCIPAL만)
    List<Portfolio> findByAccountTypeAndAcountIdAndProductType(
            AccountType accountType, String acountId, ProductType productType);

    // FLOW: 적용 시 upsert/조회
    Optional<Portfolio> findByAccountTypeAndAcountIdAndProductTypeAndProductId(
            AccountType accountType, String acountId, ProductType productType, String productId);
}
