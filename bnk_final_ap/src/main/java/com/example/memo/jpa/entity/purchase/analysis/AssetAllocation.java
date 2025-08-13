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
@Table(name = "asset_allocation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alloc_id")
    private Long allocId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "alloc_category", length = 30)
    private String allocCategory;

    @Column(name = "in_fund", precision =6, scale = 3)
    private BigDecimal inFund;

    @Column(name = "category_avg", precision =6, scale = 3)
    private BigDecimal categoryAvg;

    @Column(name = "reference_date")
    private LocalDate referenceDate;
}