package com.example.memo.jpa.entity.couple;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "irp_spouse_link")
@Getter @Setter
public class IrpSpouseLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="applicant_user_id", nullable=false) 
    private Long applicantUserId;
    @Column(name="spouse_user_id") 
    private Long spouseUserId; // 배우자 수락 전 NULL
    @Enumerated(EnumType.STRING)
    @Column(name="link_status", nullable=false, length=30)
    private LinkStatus linkStatus;

    @Column(name="applied_at")  
    private LocalDate appliedAt;
    @Column(name="approved_at") 
    private LocalDate approvedAt;
    @Column(name="unlinked_at") 
    private LocalDate unlinkedAt;
    
    @Column(name="ocr_attempt_count", nullable=false)
    private int ocrAttemptCount = 1;
}
