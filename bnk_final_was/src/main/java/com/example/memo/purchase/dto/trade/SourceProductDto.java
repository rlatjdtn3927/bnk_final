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
	private String productId; //상품 식별키
    private String productName; //PK에 해당하는 상품이름
}
