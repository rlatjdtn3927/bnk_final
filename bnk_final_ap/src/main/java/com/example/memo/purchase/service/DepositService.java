// src/main/java/com/example/memo/purchase/service/DepositService.java
package com.example.memo.purchase.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.portfolio.DepositHistory;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.jpa.repository.purchase.portfolio.DepositHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final ObjectMapper om;
    private final DepositHistoryRepository depositRepo;
    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;

    private AccountType parseAccountType(String v){
        return v==null? null : AccountType.valueOf(v.trim().toUpperCase());
    }

    /** 입금내역 조회 */
    public JsonNode getDeposits(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // 🔸 String
        return om.valueToTree(
                depositRepo.findByAccountTypeAndAcountIdOrderByDepositDateDesc(accountType, acountId));
    }

    /** 입금 처리(즉시) */
    @Transactional
    public JsonNode createDeposit(JsonNode req){
        AccountType accountType = parseAccountType(req.path("accountType").asText());
        String acountId         = req.path("acountId").asText(); // 🔸 String (IRP:계좌번호, DC:id 문자열화)
        BigDecimal amount       = req.hasNonNull("amount") ? req.get("amount").decimalValue() : BigDecimal.ZERO;
        String desc             = req.path("description").asText("");

        if(amount.signum()<=0){
            return om.createObjectNode().put("error","amount > 0 필요");
        }

        BigDecimal newBal;

        if(accountType == AccountType.IRP){
            // IRP는 계좌번호가 문자열이므로 그대로 사용
            IrpAccount a = irpRepo.findByIrpAcctNo(acountId)
                    .orElseThrow(() -> new IllegalArgumentException("IRP 계좌 없음"));
            a.setBalance((a.getBalance()==null? BigDecimal.ZERO : a.getBalance()).add(amount));
            irpRepo.save(a);
            newBal = a.getBalance();
        }else if(accountType == AccountType.DC){
            // DC는 내부적으로 Long PK이므로 파싱해서 접근, 저장은 문자열 acountId로
            Long dcId = Long.valueOf(acountId);
            DcAccount a = dcRepo.findById(dcId)
                    .orElseThrow(() -> new IllegalArgumentException("DC 계좌 없음"));
            long bal = a.getBalance()==null?0L:a.getBalance();
            a.setBalance(bal + amount.longValue());
            dcRepo.save(a);
            newBal = BigDecimal.valueOf(a.getBalance());
        }else{
            return om.createObjectNode().put("error","accountType 필요");
        }

        depositRepo.save(DepositHistory.builder()
                .accountType(accountType)  // 🔸 enum
                .acountId(acountId)        // 🔸 String
                .depositAmount(amount)
                .balanceAfter(newBal)
                .description(desc)
                .depositDate(LocalDateTime.now())
                .build());

        ObjectNode ok = om.createObjectNode();
        ok.put("status","OK");
        ok.put("balanceAfter", newBal.longValue());
        ok.put("accountType", accountType.name());
        ok.put("acountId", acountId);
        return ok;
    }
}
