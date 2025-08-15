package com.example.memo.purchase.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileTaskListDto {
    private Long taskId;
    private String prodId;
    private String prodName;
    private String prodCategory;  // fund, etf, tdf, 원리금보장상품
    private String docType;       // 투자설명서, 상품약관, 간이 투자 설명서
    private String downloadUrl;
    private Long fileSize;
}