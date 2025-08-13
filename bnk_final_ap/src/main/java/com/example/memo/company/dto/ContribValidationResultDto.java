package com.example.memo.company.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContribValidationResultDto {
    private Long batchId;
    private String fileName;
    private Integer totalRecords;
    private Integer okCount;
    private Integer errorCount;
    private List<ContribValidationItemDto> items;
}