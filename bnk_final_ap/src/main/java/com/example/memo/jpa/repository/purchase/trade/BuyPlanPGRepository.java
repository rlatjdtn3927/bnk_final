package com.example.memo.jpa.repository.purchase.trade;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanPG;
import com.example.memo.jpa.entity.user.UserEntity;

public interface BuyPlanPGRepository extends JpaRepository<BuyPlanPG, Long>{
	List<BuyPlanPG> findByUserAndIsCurrent(UserEntity user, String isCurrent);
    List<BuyPlanPG> findByUser_UserIdAndIsCurrent(Long userId, String isCurrent);
    List<BuyPlanPG> findByPrincipal_ProductIdIn(List<String> productIdList);
    List<BuyPlanPG> findByUserAndIsCurrentAndIrpAccount_IrpAcctNo(UserEntity user, String isCurrent, String accountNo);
    List<BuyPlanPG> findByUserAndIsCurrentAndDcAccount_AccountNo(UserEntity user, String isCurrent, String accountNo);
    List<BuyPlanPG> findByDcAccount_AccountNoAndPrincipal_ProductIdIn(String accountNo, List<String> productIdList);
    List<BuyPlanPG> findByIrpAccount_IrpAcctNoAndPrincipal_ProductIdIn(String accountNo, List<String> productIdList);
    List<BuyPlanPG> findByDcAccountAndIsCurrent(DcAccount account, String isCurrent);
}
