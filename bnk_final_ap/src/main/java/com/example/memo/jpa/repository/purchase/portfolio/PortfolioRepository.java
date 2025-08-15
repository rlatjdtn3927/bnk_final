// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/PortfolioRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;
import com.example.memo.jpa.entity.purchase.portfolio.Portfolio;
//보유상품 리포지토리
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
	 /** 계좌 유형/계좌ID로 보유상품 전체 조회 */
	List<Portfolio> findByAccountTypeAndAcountId(AccountType accountType, String acountId);

    /** 특정 상품(유형+상품ID)의 단건 보유내역 조회 */
	Optional<Portfolio> findByAccountTypeAndAcountIdAndProductTypeAndProductId(
            AccountType accountType, String acountId, ProductType productType, String productId);
}
