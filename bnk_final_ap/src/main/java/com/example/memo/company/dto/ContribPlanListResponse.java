package com.example.memo.company.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContribPlanListResponse {
    private List<ContribPlanListRowDto> rows;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}