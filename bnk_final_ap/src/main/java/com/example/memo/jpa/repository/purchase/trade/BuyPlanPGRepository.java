package com.example.memo.jpa.repository.purchase.trade;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.entity.user.UserEntity;

public interface BuyPlanPGRepository extends JpaRepository<BuyPlanPG, Long>{
	List<BuyPlanPG> findByUserAndIsCurrent(UserEntity user, String isCurrent);
    List<BuyPlanPG> findByUser_UserIdAndIsCurrent(Long userId, String isCurrent);
    List<BuyPlanPG> findByPrincipal_ProductIdIn(List<String> productIdList);
}
