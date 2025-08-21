package com.example.memo.purchase.reserve.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SourceProductDto {
	private String productId; //상품 식별키
    private String productName; //PK에 해당하는 상품이름
}
