// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/PortfolioRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.memo.jpa.entity.purchase.portfolio.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}
