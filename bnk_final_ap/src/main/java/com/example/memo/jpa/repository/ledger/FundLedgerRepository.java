package com.example.memo.jpa.repository.ledger;

import com.example.memo.jpa.entity.ledger.FundLedger;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FundLedgerRepository extends JpaRepository<FundLedger, Long> {

    List<FundLedger> findByIrpAccount(IrpAccount irpAccount);

    List<FundLedger> findByDcAccount(DcAccount dcAccount);

    List<FundLedger> findByFund(FundMaster fund);

}