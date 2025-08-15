package com.example.memo.purchase.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundDocumentDto {
    private Long docId;
    private String productId;     // FundMaster.productId
    private String docType;       // 투자설명서, 상품약관, 간이 투자 설명서
    private String fileUrl;
    private String status;
}