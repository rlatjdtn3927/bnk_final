package com.example.memo.admin.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subcat {
    private String code;      // URL 코드: stat-subscribers 등
    private String label;     // 화면 표기
    private String view;      // 템플릿 경로: admin/pages/stat/subscribers-report
    private String category;  // 상위카테고리: stat/sales/account
}