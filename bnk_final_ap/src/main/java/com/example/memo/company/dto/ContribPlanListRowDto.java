package com.example.memo.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContribPlanListRowDto {
    private Long batchId;
    private String planDate;       // yyyy-MM-dd
    private String status;         // DRAFT/VALIDATED/CONFIRMED/EXECUTED
    private Integer totalRecords;
    private Integer okCount;
    private Integer errorCount;
    private Long totalAmount;
    private String fileName;
    private String uploadedAt;     // yyyy-MM-dd HH:mm
}