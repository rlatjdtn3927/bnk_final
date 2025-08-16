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
@Table(name = "top5_sector")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Top5Sector extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Column(name = "sector_id")
    private Long sectorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "sector_category", length = 50)
    private String sectorCategory;

    @Column(name = "in_stock", precision = 10, scale = 4)
    private BigDecimal inStock;

    @Column(name = "category_avg", precision = 10, scale = 4)
    private BigDecimal categoryAvg;

    @Column(name = "reference_date")
    private LocalDate referenceDate;
    
    @Column(name = "status", length = 20)
    private String status;  //"INSERTED", "UPDATED"
}