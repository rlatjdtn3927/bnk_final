package com.example.memo.company.dto;

import java.time.LocalDate;

import com.example.memo.jpa.entity.company.DcContributionItem.PaymentStatus;

import lombok.Getter;

@Getter
public class PayableItemDto {
    private Long itemId;
    private String planDate;
    private String memberName;
    private String accountNo;
    private Long amount;
    private String status; // 최종적으로 '입금대기' 같은 한글명이 담길 필드

    // 이 생성자가 JPQL의 new 키워드에 의해 호출
    public PayableItemDto(Long itemId, LocalDate planDate, String memberName, String accountNo, Long amount, PaymentStatus paymentStatus) {
        this.itemId = itemId;
        this.planDate = planDate.toString();
        this.memberName = memberName;
        this.accountNo = accountNo;
        this.amount = amount;
        this.status = paymentStatus.getDisplayName(); // Enum 객체에서 한글 이름을 꺼내 저장
    }
}