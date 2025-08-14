package com.example.memo.jpa.entity.purchase.commodity_pending;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.example.memo.jpa.entity.BaseEntity;

@Entity
@Table(name = "principal_document_pending")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrincipalDocumentPending extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private PrincipalGuaranteePending principal;

    @Column(name = "doc_type", length = 20)
    private String docType; // 약관, 상품 설명서

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "status", length = 20)
    private String status;  // 예: "PENDING", "INSERTED", "UPDATED"
}