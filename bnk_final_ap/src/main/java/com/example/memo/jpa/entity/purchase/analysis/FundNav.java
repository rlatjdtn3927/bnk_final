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
@Table(name = "fund_nav")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundNav extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fund_nav_id")
    private Long fundNavId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "nav", precision = 15, scale = 4)
    private BigDecimal nav;

    @Column(name = "reference_date")
    private LocalDate referenceDate;
    
    @Column(name = "status", length = 20)
    private String status;  //"INSERTED", "UPDATED"
}

