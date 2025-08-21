package com.example.memo.jpa.entity.ledger;

import java.math.BigDecimal;

import com.example.memo.jpa.entity.BaseEntity;
import com.example.memo.jpa.entity.company.DcAccount;
import com.example.memo.jpa.entity.irp.IrpAccount;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fund_holding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FundHoldings extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "irp_account_id", nullable = false)
    private IrpAccount irpAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dc_account_id", referencedColumnName = "account_no", nullable = false)
    private DcAccount  dcAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    /**
     * 보유 좌수 (누적 매수 좌수 – 매도 좌수) --> 0이 되면 해당 행 삭제
     */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal units;

    /**
     * 평균 단가
     * = (Σ 매수금액) ÷ (Σ 매수좌수)
     * 단, 매도 시에는 변동 없음
     */
    @Column(precision = 18, scale = 4)
    private BigDecimal avgPrice;

    /**
     * 매수 원금 (잔여 원가)
     * = avgPrice × units
     * 또는 = 기존 매수원금 + 신규매입금액 – (평균단가 × 매도좌수)
     */
    @Column(precision = 18, scale = 2)
    private BigDecimal acquisitionAmount;

    /**
     * 평가액
     * = units × 현재 기준가(NAV)
     */
    @Column(precision = 18, scale = 2)
    private BigDecimal valuationAmount;

    /**
     * 평가손익
     * = valuationAmount – acquisitionAmount
     */
    @Column(precision = 18, scale = 2)
    private BigDecimal profitLoss;

    /**
     * 수익률 (%)
     * = (profitLoss ÷ acquisitionAmount) × 100
     * 단, acquisitionAmount = 0일 경우 0 처리
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal returnRate;
}
