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

@Entity
@Table(name="link_admin_review")
@Getter @Setter
public class LinkAdminReview {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="link_id", nullable=false) 
    private Long linkId;
    
    @Column(name="admin_id") 
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name="review_status", nullable=false, length=20)
    private ReviewStatus reviewStatus;

    @Column(name="reason")     
    private String reason;
    
    @Column(name="admin_memo") 
    private String adminMemo;

    @Column(name="requested_at") 
    private LocalDate requestedAt;
    
    @Column(name="completed_at") 
    private LocalDate completedAt;
}
