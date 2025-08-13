package com.example.memo.jpa.entity.company;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.memo.jpa.entity.admin.BankEmployee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dc_account_request")
@Getter @Setter
public class DcAccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status")
    private String status; // 예: "PENDING", "APPROVED", "REJECTED"

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // N:1 관계 - 여러 요청이 한 명의 가입자에게 속할 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dc_member_id")
    private DcMember dcMember;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private CompanyManager requestedBy; // 신청한 기업 담당자 엔티티
    
    
    // N:1 관계 - 여러 요청을 한 명의 관리자가 처리할 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private BankEmployee approvedBy; // 처리한 은행 직원 엔티티
    
    // 1:1 관계 - 하나의 요청이 승인되면 하나의 계좌와 연결됨
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dc_account_id", unique = true)
    private DcAccount dcAccount;
}