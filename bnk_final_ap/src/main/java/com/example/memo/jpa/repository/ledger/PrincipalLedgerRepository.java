package com.example.memo.jpa.repository.ledger;

import com.example.memo.jpa.entity.ledger.PrincipalLedger;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrincipalLedgerRepository extends JpaRepository<PrincipalLedger, Long> {

    List<PrincipalLedger> findByIrpAccount(IrpAccount irpAccount);

    List<PrincipalLedger> findByDcAccount(DcAccount dcAccount);

    List<PrincipalLedger> findByPrincipal(PrincipalGuarantee principal);

    List<PrincipalLedger> findByStatus(String status);
}
