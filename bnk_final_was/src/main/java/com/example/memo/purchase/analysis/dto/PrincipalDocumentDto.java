package com.example.memo.purchase.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrincipalDocumentDto {
    private Long docId;
    private String productId;     // PrincipalGuarantee.productId
    private String docType;       // 약관, 상품 설명서
    private String fileUrl;
}