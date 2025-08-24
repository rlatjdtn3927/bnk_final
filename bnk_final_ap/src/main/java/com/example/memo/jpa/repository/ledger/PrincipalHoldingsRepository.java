package com.example.memo.jpa.repository.ledger;

import com.example.memo.jpa.entity.ledger.PrincipalHoldings;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrincipalHoldingsRepository extends JpaRepository<PrincipalHoldings, Long> {

    List<PrincipalHoldings> findByIrpAccount(IrpAccount irpAccount);

    List<PrincipalHoldings> findByDcAccount(DcAccount dcAccount);
    
    List<PrincipalHoldings> findByDcAccount_AccountNo(String accountNo);

    List<PrincipalHoldings> findByPrincipal(PrincipalGuarantee principal);

    List<PrincipalHoldings> findByStatus(String status);
}
