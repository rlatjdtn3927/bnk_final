// src/main/java/com/example/memo/jpa/repository/purchase/portfolio/TransactionHistoryRepository.java
package com.example.memo.jpa.repository.purchase.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.memo.jpa.entity.purchase.portfolio.TransactionHistory;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
}
