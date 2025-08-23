package com.example.memo.jpa.entity.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;

@Entity
@Table(name = "principal_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrincipalLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "irp_account_id")
    private IrpAccount irpAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dc_account_id", referencedColumnName = "account_no")
    private DcAccount  dcAccount;

    // 원리금 보장상품 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private PrincipalGuarantee principal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal contractAmount; // 가입 원금

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate; // 약정 금리 (%)

    @Column(nullable = false)
    private LocalDate startDate; // 계약 시작일

    @Column(nullable = false)
    private LocalDate maturityDate; // 만기일

    @Column(precision = 18, scale = 2)
    private BigDecimal interestAccrued; // 현재까지 발생한 이자 (중도 or 만기)

    @Column(nullable = false)
    private String status; // 계약 상태 (ACTIVE, MATURED, TERMINATED)

}