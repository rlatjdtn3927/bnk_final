package com.example.memo.purchase.dto.portfolio;

import lombok.Data;
import java.util.List;

@Data
public class PortfolioChangeDto {
    private String accountType;
    private String acountId;
    private List<Line> lines; // 상품별 목표비중/증감 등

    @Data
    public static class Line {
        private String productType;  // FUND/ETF/TDF/PRINCIPAL
        private String productId;
        private String action;       // BUY/SELL/HOLD
        private String quantity;     // 문자열로 두고 서버에서 숫자 변환
    }
}
