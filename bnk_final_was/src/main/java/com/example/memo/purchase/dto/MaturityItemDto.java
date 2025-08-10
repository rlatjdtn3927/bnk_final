package com.example.memo.purchase.dto;

import lombok.AllArgsConstructor; 
import lombok.Data;
import java.time.LocalDate;

@Data 
@AllArgsConstructor
public class MaturityItemDto {
    private String category;
    private String name;
    private LocalDate maturityDate;
    private String plan; // 만기처리 계획
}
