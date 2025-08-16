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

/* 4-1) 펀드 문서 – fund_document */
@Entity
@Table(name = "fund_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundDocument extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private FundMaster fund;

    @Column(name = "doc_type", length = 20)
    private String docType; // 투자설명서, 상품약관, 간이 투자 설명서

    @Column(name = "file_url", length = 500)
    private String fileUrl;
    
    @Column(name = "status", length = 20)
    private String status;  //"INSERTED", "UPDATED"
}