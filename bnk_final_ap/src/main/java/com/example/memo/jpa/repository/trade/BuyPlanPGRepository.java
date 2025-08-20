package com.example.memo.jpa.repository.trade;

import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BuyPlanPGRepository extends JpaRepository<BuyPlanPG, Long> {
    List<BuyPlanPG> findByUser_UserIdAndIsCurrent(Long userId, String isCurrent);
}
