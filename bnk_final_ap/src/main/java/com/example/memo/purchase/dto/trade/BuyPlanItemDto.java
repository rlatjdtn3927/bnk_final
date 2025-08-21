package com.example.memo.purchase.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuyPlanItemDto {
    private Long planItemId;
    private Long userId;
    private String productId;
    private Integer allocationPercent;
    private String isCurrent;
}