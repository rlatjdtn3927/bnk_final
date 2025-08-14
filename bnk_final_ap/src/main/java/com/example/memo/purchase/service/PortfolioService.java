package com.example.memo.purchase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final ObjectMapper om;
    // TODO: 레포지토리 주입
    // private final PortfolioRepository portfolioRepo;
    // private final TransactionHistoryRepository txnRepo;
    // private final FundMasterRepository fundRepo;
    // private final PrincipalGuaranteeRepository principalRepo;

    public JsonNode list(JsonNode req) {
        // TODO: accountType+acountId로 보유목록 조회
        return om.createArrayNode();
    }

    public JsonNode changePreview(JsonNode req) {
        // TODO: 수량/금액 변화, 수수료/체결가정 등을 계산한 미리보기 결과
        ObjectNode out = om.createObjectNode();
        out.put("status", "PREVIEW");
        out.put("message", "미리보기 결과 (샘플)");
        return out;
    }

    @Transactional // 승인 없이 즉시 반영
    public JsonNode changeApply(JsonNode req) {
        // 1) 요청 파싱: accountType, acountId, lines[{productType, productId, action, quantity}]
        // 2) 현재 포지션 조회 → 증감 반영
        // 3) TransactionHistory(BUY/SELL) 기록
        // 4) Portfolio 수량/평가액 업데이트
        // 5) 필요시 계좌 잔액 조정

        ObjectNode out = om.createObjectNode();
        out.put("status", "APPLIED");
        out.put("message", "보유상품 변경이 즉시 반영되었습니다.");
        out.set("portfolio", om.createArrayNode()); // 최신 스냅샷 (TODO 실제 데이터)
        return out;
    }
}
