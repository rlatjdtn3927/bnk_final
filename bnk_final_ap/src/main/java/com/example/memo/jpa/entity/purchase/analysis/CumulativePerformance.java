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
@Table(name = "cumulative_performance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CumulativePerformance extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "perf_id")
    private Long perfId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "period_code", length = 10)
    private String periodCode;   // ENUM → String

    @Column(name = "fund_return", precision = 10, scale = 4)
    private BigDecimal fundReturn;

    @Column(name = "bm_return", precision = 10, scale = 4)
    private BigDecimal bmReturn;

    @Column(name = "category_avg_ret", precision = 10, scale = 4)
    private BigDecimal categoryAvgRet;

    @Column(name = "reference_date")
    private LocalDate referenceDate;
}