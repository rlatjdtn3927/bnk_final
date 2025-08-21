package com.example.memo.couple;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper om;

    /**
     * 현재 로그인한 사용자의 안 읽은 알림 목록을 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        // AP로 보낼 payload 생성
        ObjectNode payload = om.createObjectNode();
        payload.put("userId", userId);

        // TcpMessage 객체를 생성하여 AP로 전송
        TcpMessage message = new TcpMessage(Command.NOTIFICATION_GET_UNREAD, payload);
        JsonNode apResponse = tcpClientService.sendMessage(message);
        
        return ResponseEntity.ok(apResponse);
    }

    /**
     * 특정 알림을 '읽음'으로 처리
     * @param notificationId 읽음 처리할 알림의 ID
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") Long notificationId) {
        // AP로 보낼 payload 생성
        ObjectNode payload = om.createObjectNode();
        payload.put("notificationId", notificationId);

        // TcpMessage 객체를 생성하여 AP로 전송
        TcpMessage message = new TcpMessage(Command.NOTIFICATION_MARK_AS_READ, payload);
        JsonNode apResponse = tcpClientService.sendMessage(message);

        return ResponseEntity.ok(apResponse);
    }
    
    /**
     * 현재 로그인한 사용자의 모든 알림 목록을 조회
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllNotifications(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        ObjectNode payload = om.createObjectNode();
        payload.put("userId", userId);

        TcpMessage message = new TcpMessage(Command.NOTIFICATION_GET_ALL, payload);
        JsonNode apResponse = tcpClientService.sendMessage(message);
        
        return ResponseEntity.ok(apResponse);
    }
}