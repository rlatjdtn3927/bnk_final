package com.example.memo.purchase.dto.commodity;

	
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DefaultOptionDto {
    private String defaultId;
    private String optionName;
    private Integer riskGradeNum;
    private String riskGradeText;
    private String subProd1;
    private String subProd2;
    private String descUrl;
    private String guideUrl;
    private Integer stblRate;
    private Integer nvstRate;

    // BaseEntity 공통 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

