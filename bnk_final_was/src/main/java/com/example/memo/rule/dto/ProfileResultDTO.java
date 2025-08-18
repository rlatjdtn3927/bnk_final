package com.example.memo.rule.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResultDTO {
    private Long userId;
    private Long typeId;
    private int totalScore;
}
