package com.example.memo.jpa.repository.purchase.trade;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.trade.BuyPlanFund;
import com.example.memo.jpa.entity.user.UserEntity;

public interface BuyPlanFundRepository extends JpaRepository<BuyPlanFund, Long>{
	List<BuyPlanFund> findByUserAndIsCurrent(UserEntity user, String isCurrent);
	List<BuyPlanFund> findByUser_UserIdAndIsCurrent(Long userId, String isCurrent);
	List<BuyPlanFund> findByFund_ProductIdIn(List<String> prodIdList);
	List<BuyPlanFund> findByUserAndIsCurrentAndIrpAccount_IrpAcctNo(UserEntity user, String isCurrent, String accountNo);
	List<BuyPlanFund> findByUserAndIsCurrentAndDcAccount_AccountNo(UserEntity user, String isCurrent, String accountNo);
	List<BuyPlanFund> findByIrpAccount_IrpAcctNoAndFund_ProductIdIn(String accountNo, List<String> prodIdList);
	List<BuyPlanFund> findByDcAccount_AccountNoAndFund_ProductIdIn(String accountNo, List<String> prodIdList);
	List<BuyPlanFund> findByDcAccountAndIsCurrent(DcAccount accountNo, String isCurrent);
	List<BuyPlanFund> findByIrpAccountAndIsCurrent(IrpAccount accountNo, String isCurrent);
}
