package com.example.memo.purchase.dto.trade;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SourceProductDto {
	private String productId;
    private String productName;
}
