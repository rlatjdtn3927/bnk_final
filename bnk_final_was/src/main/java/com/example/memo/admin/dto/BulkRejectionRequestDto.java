package com.example.memo.admin.dto;

import java.util.List;

import lombok.Data;

@Data
public class BulkRejectionRequestDto {
    private List<Long> requestIds;
    private String reason;
}
