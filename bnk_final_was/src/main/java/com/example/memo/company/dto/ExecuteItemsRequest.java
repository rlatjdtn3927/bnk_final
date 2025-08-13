package com.example.memo.company.dto;

import java.util.List;

import lombok.Data;

/* 요청 Body 를 받기 위한 dto 클래스 */
@Data
public class ExecuteItemsRequest {
    private List<Long> itemIds;
    private Long sourceAccountId;
}
