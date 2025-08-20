package com.example.memo.jpa.entity.couple;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="notification")
@Getter @Setter
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false) // 알림을 받을 사용자의 ID
    private Long userId;

    @Column(name="message", nullable=false) // 알림 내용
    private String message;

    @Column(name="is_read", nullable=false) // 읽음 여부 (핵심!)
    private boolean isRead = false; // 기본값은 '안 읽음'

    @Column(name="link_url") // 알림 클릭 시 이동할 URL (선택 사항)
    private String linkUrl;

    @CreationTimestamp // JPA 기능으로 자동 생성 시간 기록
    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt;
    
    @Column(name="link_id") // DB 컬럼명은 link_id
    private Long linkId;    // 연관된 irp_spouse_link의 ID
}