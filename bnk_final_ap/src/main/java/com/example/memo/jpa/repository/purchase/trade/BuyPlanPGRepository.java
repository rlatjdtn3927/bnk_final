package com.example.memo.jpa.repository.purchase.trade;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.entity.user.User;

public interface BuyPlanPGRepository extends JpaRepository<BuyPlanPG, Long>{
	List<BuyPlanPG> findByUserAndIsCurrent(User user, String isCurrent);
}
