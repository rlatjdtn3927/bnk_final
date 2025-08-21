package com.example.memo.couple.handler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.memo.couple.service.NotificationService;
import com.example.memo.jpa.entity.couple.Notification;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationHandler implements TcpMessageHandler {

    private final NotificationService notificationService;
    private final ObjectMapper om;

    @Override
    public boolean supports(Command c) {
        // 'NOTIFICATION_'으로 시작하는 모든 명령을 이 핸들러가 처리
        return c.name().startsWith("NOTIFICATION_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                // '안 읽은 알림 목록 조회' 요청 처리
                case NOTIFICATION_GET_UNREAD: {
                    Long userId = data.path("userId").asLong();
                    List<Notification> notifications = notificationService.findUnreadByUserId(userId);

                    ObjectNode res = om.createObjectNode();
                    res.put("success", true).put("code", "OK");
                    // List<Notification>을 JSON 배열로 변환하여 data 필드에 담아줍니다.
                    res.set("data", om.valueToTree(notifications));
                    return res;
                }

                // '알림 읽음 처리' 요청 처리
                case NOTIFICATION_MARK_AS_READ: {
                    Long notificationId = data.path("notificationId").asLong();
                    notificationService.markAsRead(notificationId);

                    ObjectNode res = om.createObjectNode();
                    res.put("success", true).put("code", "OK");
                    res.put("message", "알림을 읽음 처리했습니다.");
                    return res;
                }
                case NOTIFICATION_GET_ALL: {
                    Long userId = data.path("userId").asLong();
                    List<Notification> notifications = notificationService.findAllByUserId(userId);
                    ObjectNode res = om.createObjectNode();
                    res.put("success", true).put("code", "OK");
                    res.set("data", om.valueToTree(notifications));
                    return res;
                }

                default:
                    return "알 수 없는 명령: " + command.name();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ObjectNode err = om.createObjectNode();
            err.put("success", false).put("code", "ERROR").put("message", e.getMessage());
            return err;
        }
    }
}