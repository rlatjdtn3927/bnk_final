// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/DepositHistoryRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.memo.jpa.entity.purchase.portfolio.DepositHistory;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
}
