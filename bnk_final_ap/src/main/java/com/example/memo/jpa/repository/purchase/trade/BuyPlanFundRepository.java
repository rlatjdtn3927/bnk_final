package com.example.memo.jpa.repository.purchase.trade;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.user.User;

public interface BuyPlanFundRepository extends JpaRepository<BuyPlanFund, Long>{
	List<BuyPlanFund> findByUserAndIsCurrent(User user, String isCurrent);
}
