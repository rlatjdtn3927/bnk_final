// src/main/java/com/example/memo/purchase/service/TradePrereqService.java
package com.example.memo.purchase.service;

import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradePrereqService {

    private final ObjectMapper om;
    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;

    private AccountType parseAccountType(String v){
        return v==null? null : AccountType.valueOf(v.trim().toUpperCase());
    }

    public JsonNode prereqCheck(JsonNode req){
        AccountType type  = parseAccountType(req.path("accountType").asText());
        String acIdStr    = req.path("acountId").asText();
        String userIdStr  = req.path("userId").asText();

        ObjectNode out = om.createObjectNode();
        out.put("userValid", userIdStr!=null && !userIdStr.isEmpty());

        BigDecimal balance = BigDecimal.ZERO;
        boolean exists = false;

        if(type == AccountType.IRP){
            exists = irpRepo.findByIrpAcctNo(acIdStr).isPresent();
            balance = irpRepo.findByIrpAcctNo(acIdStr)
                    .map(a -> a.getBalance()==null? BigDecimal.ZERO : a.getBalance())
                    .orElse(BigDecimal.ZERO);
            out.put("hasIrpAccount", exists);
        }else if(type == AccountType.DC){
            try{
                Long id = Long.valueOf(acIdStr);
                exists = dcRepo.findById(id).isPresent();
                balance = dcRepo.findById(id)
                        .map(a -> BigDecimal.valueOf(a.getBalance()==null?0L:a.getBalance()))
                        .orElse(BigDecimal.ZERO);
                out.put("hasDcAccount", exists);
            }catch(Exception ignore){ exists=false; }
        }

        out.put("balance", balance.longValue());
        out.put("hasBalance", balance.signum()>0);
        out.put("accountType", type==null? "": type.name());
        return out;
    }
}
