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
@Table(name = "risk_metric")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskMetric extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long metricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "metric_name", length = 20)
    private String metricName;    // ENUM → String

    @Column(name = "period_code", length = 10)
    private String periodCode;    // ENUM → String

    @Column(name = "metric_value", precision = 10, scale = 4)
    private BigDecimal metricValue;

    @Column(name = "percentile_rank", precision = 10, scale = 4)
    private BigDecimal percentileRank;

    @Column(name = "category_avg", precision = 10, scale = 4)
    private BigDecimal categoryAvg;

    @Column(name = "reference_date")
    private LocalDate referenceDate;
    
    @Column(name = "status", length = 20)
    private String status;  //"INSERTED", "UPDATED"
}