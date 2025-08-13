package com.example.memo.jpa.entity.purchase.analysis;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.example.memo.jpa.entity.BaseEntity;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;

@Entity
@Table(name = "principal_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrincipalDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private PrincipalGuarantee principal;

    @Column(name = "doc_type", length = 20)
    private String docType; // 약관, 상품 설명서

    @Column(name = "file_url", length = 500)
    private String fileUrl;
}