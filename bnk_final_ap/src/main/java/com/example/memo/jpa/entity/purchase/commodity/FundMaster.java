package com.example.memo.jpa.entity.purchase.commodity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.memo.jpa.entity.BaseEntity;

/* 1) 펀드 기본정보 – fund_master */
@Entity
@Table(name = "fund_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundMaster extends BaseEntity{

    @Id
    @Column(name = "product_id", length = 30)
    private String productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "risk_grade_num")
    private Byte riskGradeNum;

    @Column(name = "risk_grade_text", length = 20)
    private String riskGradeText;      // 고위험, 저위험 등등..

    @Column(name = "fund_type", length = 20)
    private String fundType;  // 주식형, 채권형 등등...

    @Column(name = "inception_date")
    private LocalDate inceptionDate;

    @Column(name = "management_company", length = 100)
    private String managementCompany;

    @Column(name = "total_expense_ratio", precision = 5, scale = 2)
    private BigDecimal totalExpenseRatio;

    @Column(name = "category", length = 10)
    private String category; // fund, etf, tdf

    @Column(name = "channel")
    private Integer channel;
    
    @Column(name = "status", length = 20)
    private String status;  //"INSERTED", "UPDATED"
}

//fundMasterRepository.deleteById("abc");
//entityManager.flush();  // 쿼리 반영
//entityManager.clear();  // 캐시 비우기 (DB 상태와 동기화)