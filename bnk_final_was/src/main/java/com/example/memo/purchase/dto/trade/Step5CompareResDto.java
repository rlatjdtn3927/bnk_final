package com.example.memo.purchase.dto.trade;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Step5CompareResDto {
    private String flow;
    private List<CompareItemDto> before;
    private List<CompareItemDto> after;
    private String message;

    // 상단 배지 등 보조 정보
    private String riskGrade;
    private Integer riskGradeNum;

    public static Step5CompareResDto error(String msg){
        return new Step5CompareResDto(null, List.of(), List.of(), msg, null, null);
    }
}
