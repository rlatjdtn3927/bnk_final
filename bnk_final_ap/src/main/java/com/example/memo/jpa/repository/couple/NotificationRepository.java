package com.example.memo.jpa.repository.couple;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.memo.jpa.entity.couple.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 특정 사용자의 읽지 않은 알림 목록을 최신순으로 조회
     *
     * @param userId 조회할 사용자의 ID
     * @return 읽지 않은 알림 엔티티 목록
     */
    List<Notification> findByUserIdAndIsReadIsFalseOrderByIdDesc(Long userId);
    Optional<Notification> findFirstByLinkIdAndIsReadIsFalseOrderByIdDesc(Long linkId);
    
    List<Notification> findAllByUserIdOrderByIdDesc(Long userId);

}