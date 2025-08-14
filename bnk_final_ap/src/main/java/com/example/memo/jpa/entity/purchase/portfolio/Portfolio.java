package com.example.memo.jpa.entity.purchase.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.memo.jpa.entity.BaseEntity;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.ProductType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portfolio") // DB가 PORFOLIO면 "porfolio"로 변경
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Portfolio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 10, nullable = false)
    private AccountType accountType; // IRP / DC

    // 단일 컬럼으로 계좌 식별자 저장 (IRP: irp_acct_no(String), DC: dc_account.id(Long))
    @Column(name = "acount_id", nullable = false, length = 30) // 스키마 컬럼명 그대로(acount_id)
    private String acountId;

    @Column(name = "product_id", length = 30, nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 20, nullable = false)
    private ProductType productType;

    @Column(name = "quantity", precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "invest_amt", precision = 15, scale = 0)
    private BigDecimal investAmt;

    @Column(name = "eval_amt", precision = 15, scale = 0)
    private BigDecimal evalAmt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    // ---- 선택적 읽기 전용 참조 (productId 공유) ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id",
                insertable = false, updatable = false)
    private FundMaster fund; // productType == FUND/ETF/TDF일 때 의미

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id",
                insertable = false, updatable = false)
    private PrincipalGuarantee principal; // productType == PRINCIPAL일 때 의미
}
