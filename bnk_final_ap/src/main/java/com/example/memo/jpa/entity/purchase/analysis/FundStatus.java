package com.example.memo.jpa.entity.purchase.analysis;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.example.memo.jpa.entity.BaseEntity;
import com.example.memo.jpa.entity.purchase.commodity.FundMaster;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fund_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundStatus extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "valuation_type", length = 50)
    private String valuationType;

    @Column(name = "management_company", length = 200)
    private String managementCompany;

    @Column(name = "invest_area", length = 50)
    private String investArea;

    @Column(name = "track_record_1y", length = 200)
    private String trackRecord1y;

    @Column(name = "track_record_2y", length = 200)
    private String trackRecord2y;

    @Column(name = "expense_ratio_1y", length = 200)
    private String expenseRatio1y;

    @Column(name = "expense_ratio_3y", length = 200)
    private String expenseRatio3y;

    private String launchDate;

    @Column(precision = 5, scale = 2)
    private BigDecimal salesFee;

    @Column(name = "management_scale", length = 100)
    private String managementScale;

    @Column(precision = 5, scale = 2)
    private BigDecimal trustFee;

    @Column(name = "net_asset", length = 50)
    private String netAsset;

    @Column(precision = 5, scale = 2)
    private BigDecimal redemptionFee;

    @Column(name = "reference_date")
    private LocalDate referenceDate;
}