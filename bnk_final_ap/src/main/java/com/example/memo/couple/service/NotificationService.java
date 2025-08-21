package com.example.memo.couple.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.jpa.entity.couple.Notification;
import com.example.memo.jpa.repository.couple.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 특정 사용자의 읽지 않은 알림 목록을 조회
     * @param userId 조회할 사용자의 ID
     * @return 읽지 않은 알림 목록
     */
    @Transactional(readOnly = true) 
    public List<Notification> findUnreadByUserId(Long userId) {
        return notificationRepository.findByUserIdAndIsReadIsFalseOrderByIdDesc(userId);
    }

    /**
     * 특정 알림을 '읽음' 상태로 변경
     * @param notificationId 읽음 처리할 알림의 ID
     */
    @Transactional // 데이터 변경이 일어나므로 @Transactional을 사용
    public void markAsRead(Long notificationId) {
        // ID로 알림을 찾고, 없으면 예외를 발생시킵니다.
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다. ID: " + notificationId));
        
        notification.setRead(true);
        // @Transactional에 의해 메소드가 종료될 때 변경된 내용을 감지(Dirty Checking)하여 자동으로 UPDATE 쿼리를 실행
    }

    /**
     * 새로운 알림을 생성하고 DB에 저장
     * @param userId 알림을 받을 사용자 ID
     * @param message 알림 내용
     * @param linkUrl 알림 클릭 시 이동할 URL
     * @return 생성된 알림 엔티티
     */
    @Transactional
    public Notification createNotification(Long userId, String message, String linkUrl, Long linkId) {
        Notification noti = new Notification();
        noti.setUserId(userId);
        noti.setMessage(message);
        noti.setLinkUrl(linkUrl);
        noti.setLinkId(linkId);
        return notificationRepository.save(noti);
    }
    
    @Transactional(readOnly = true)

    public List<Notification> findAllByUserId(Long userId) {

        return notificationRepository.findAllByUserIdOrderByIdDesc(userId);

    }
}