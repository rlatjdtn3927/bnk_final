package com.example.memo.jpa.entity.ledger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class FundLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "irp_account_id", nullable = false)
    private IrpAccount irpAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dc_account_id", referencedColumnName = "accountNo", nullable = false)
    private DcAccount  dcAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(nullable = false)
    private String tradeType; // 거래유형 (BUY, SELL, SWITCH_OUT 등)

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal tradeUnits; // 거래 좌수

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal tradePrice; // 거래 단가 (좌당 가격)

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal tradeAmount; // 거래금액 (좌수×단가)

    @Column(nullable = false)
    private LocalDateTime tradeDate; // 거래일시
}
