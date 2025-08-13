package com.example.memo.company.dto;

import java.time.LocalDate;

import com.example.memo.jpa.entity.company.DcContributionBatch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayableItemDto {
    private Long itemId;
    private DcContributionBatch.BatchStatus status; // 진행상태
    private String memberName; // 가입자명(회원명)
    private String accountNo;  // 계좌번호
    private LocalDate planDate; // 입금예정일
    private Long amount;       // 기업부담금
}