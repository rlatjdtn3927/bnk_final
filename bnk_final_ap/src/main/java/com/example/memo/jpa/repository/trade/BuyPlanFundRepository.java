package com.example.memo.jpa.repository.trade;

import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BuyPlanFundRepository extends JpaRepository<BuyPlanFund, Long> {
    List<BuyPlanFund> findByUser_UserIdAndIsCurrent(Long userId, String isCurrent);
}
