package com.example.memo.purchase.change.dto.response;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrincipalLedgerDto {

    private Long id;

    private String irpAccountId;    // IrpAccount FK -> id
    private String dcAccountNo;   // DcAccount FK -> account_no
    private String principalId;   // PrincipalGuarantee FK -> product_id

    private BigDecimal contractAmount; // 가입 원금
    private BigDecimal interestRate;   // 약정 금리
    private LocalDate startDate;       // 계약 시작일
    private LocalDate maturityDate;    // 만기일
    private BigDecimal interestAccrued;// 현재까지 발생한 이자
    private String status;             // 계약 상태
}