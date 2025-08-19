package com.example.memo.purchase.dto.commodity;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DOSpecificDto {
    private Long doSpecificId;      // PK
    private String defaultId;       // 연관 DefaultOption의 ID
    private Integer consRate;
    private String consName;
    private String consCategory;
    private String offerCompany;
    private String simpleUrl;
    private String descUrl;
    private String termsUrl;

    // BaseEntity 공통 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
