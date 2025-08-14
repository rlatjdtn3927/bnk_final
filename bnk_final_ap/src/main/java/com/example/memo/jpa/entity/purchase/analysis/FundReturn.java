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
@Table(name = "fund_return")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundReturn extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "return_type", length = 10)
    private String returnType;   // 펀드, 유형평균

    @Column(name = "return_value", precision = 10, scale = 4)
    private BigDecimal returnValue;

    @Column(name = "reference_date")
    private LocalDate referenceDate;
    
    @Column(name = "status", length = 20)
    private String status;  //"INSERTED", "UPDATED"
}