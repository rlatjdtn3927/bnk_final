package com.example.memo.purchase.dto;

import lombok.AllArgsConstructor; 
import lombok.Data;
import java.time.LocalDateTime;

@Data 
@AllArgsConstructor
public class TxItemDto {
    private LocalDateTime at;
    private String type;       // 매수/매도/가입 등
    private String name;       // 상품명
    private int qty;           // 수량(예금은 1)
    private long price;        // 단가
    private long total;        // 체결금액
}
