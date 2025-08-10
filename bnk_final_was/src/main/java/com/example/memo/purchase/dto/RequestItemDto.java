package com.example.memo.purchase.dto;

import lombok.AllArgsConstructor; 
import lombok.Data;
import java.time.LocalDateTime;

@Data 
@AllArgsConstructor
public class RequestItemDto {
    private String type;       // 보유상품 변경 등
    private String detail;     // 변경 내용
    private LocalDateTime requestedAt;
    private String status;     // 접수/승인/취소
}
