package com.example.memo.purchase.trade_common.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeValueDto {
    /* 공통 */
    private String flow;
    private String accountId;     // IRP: irp_acct_no / DC: account_no
    private String accountType;   // "IRP" | "DC"
    private String riskGrade;
    private Integer riskGradeNum;

    /* Step2: 매도 선택 결과 */
    private List<SoldItem> soldItems;
    private BigDecimal      soldTotalAmount; // 컨트롤러에서 합산 저장

    /* Step2: 매수 선택 결과 */
    private List<BuyItem>   buyItems;        // step2_changeProdList/step2_buyList에서 저장
    private BigDecimal      buyTotalAmount;
    
    // step4 서류 동의
    private String fileUrlList;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SoldItem {
        /** "FUND" | "PRINCIPAL" */
        private String type;

        /** FUND일 때 */
        private String prodId;     // Fund productId
        /** PRINCIPAL일 때 */
        private Long   ledgerId;   // PrincipalLedgerDto.id
        
        private String holdingId; // ✅ FundHolding.id (보유펀드 PK)
        private Long   id;  // PrincipalLedger.id

        private String name;       // 화면 표시용
        private Integer ratio;     // 팔 비율(%)
        private BigDecimal amount; // 화면에서 계산한 매도금액(합계 표시에 사용)
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BuyItem {
        /** "FUND" | "PRINCIPAL" */
        private String type;

        private String prodId;     // 살 상품 productId (정기예금도 productId로 통일)
        private String name;       // 화면 표시용
        private Integer ratio;     // 비율(%), step2_buyList에서 100% 검증용 (옵션)
        private BigDecimal amount; // 매수금액(원) — 합계=매도합계로 맞춤
    }
}
