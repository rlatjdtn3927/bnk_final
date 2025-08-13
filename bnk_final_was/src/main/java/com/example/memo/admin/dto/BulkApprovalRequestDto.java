package com.example.memo.admin.dto;

import java.util.List;

import lombok.Data;

@Data
public class BulkApprovalRequestDto {
    private List<Long> requestIds;
}
