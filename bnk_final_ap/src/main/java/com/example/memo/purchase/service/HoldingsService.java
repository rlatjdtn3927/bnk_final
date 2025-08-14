package com.example.memo.purchase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class HoldingsService {

    private final ObjectMapper om;

    // TODO: 필요한 레포지토리 주입
    // private final DepositHistoryRepository depositRepo;
    // private final TransactionHistoryRepository txnRepo;
    // private final PortfolioRepository portfolioRepo;
    // private final IrpAccountRepository irpRepo;
    // private final DcAccountRepository dcRepo;

    public JsonNode getSummary(JsonNode req) {
        // TODO: 총평가/수익률/입금합/당일입금/운용수익 계산
        ObjectNode out = om.createObjectNode();
        out.put("totalEvalAmt", 0);
        out.put("totalProfitRate", 0);
        out.put("totalDepositAmt", 0);
        out.put("todayDepositAmt", 0);
        out.put("opProfitAmt", 0);
        return out;
    }

    public JsonNode getTransactions(JsonNode req) {
        // TODO: 거래내역 조회
        return om.createArrayNode();
    }

    public JsonNode getDeposits(JsonNode req) {
        // TODO: 입금내역 조회
        return om.createArrayNode();
    }

    @Transactional // 승인 없이 즉시 반영
    public JsonNode createDeposit(JsonNode req) {
        final String accountType = req.path("accountType").asText();
        final String acountId    = req.path("acountId").asText();
        final BigDecimal amount  = req.hasNonNull("amount") ? req.get("amount").decimalValue() : BigDecimal.ZERO;
        final String description = req.path("description").asText("");

        // TODO:
        // 1) accountType에 따라 IRP/DC 계좌 잔액 += amount
        // 2) DepositHistory 저장 (depositDate가 없으면 now)
        // 3) 필요시 todayDepositAmt 갱신 로직

        ObjectNode snapshot = om.createObjectNode();
        snapshot.put("status", "OK");
        snapshot.put("accountType", accountType);
        snapshot.put("acountId", acountId);
        snapshot.put("updatedBalance", 0); // TODO 실제 잔액
        snapshot.put("depositAmount", amount);
        snapshot.put("message", "입금이 즉시 반영되었습니다.");
        return snapshot;
    }
}
