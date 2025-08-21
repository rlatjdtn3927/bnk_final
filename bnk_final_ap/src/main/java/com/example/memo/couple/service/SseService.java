package com.example.memo.couple.service;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SseService {

    // 현재 연결된 사용자들의 Emitter를 저장하는 맵 (Thread-safe한 자료구조 사용)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새로운 사용자가 SSE 연결을 구독합니다.
     * @param userId 구독할 사용자의 ID
     * @return 생성된 SseEmitter 객체
     */
    public SseEmitter subscribe(Long userId) {
        // 1. 새로운 SseEmitter 객체 생성 (10분 타임아웃)
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        // 2. 생성된 Emitter를 맵에 저장
        emitters.put(userId, emitter);
        log.info("새로운 Emitter 연결: userId={}", userId);

        // 3. 연결이 종료되거나 타임아웃될 때 맵에서 제거하도록 콜백 설정
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("Emitter 연결 종료: userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("Emitter 타임아웃: userId={}", userId);
        });
        emitter.onError(e -> {
            emitters.remove(userId);
            log.error("Emitter 오류 발생: userId={}", userId, e);
        });

        // 4. 연결 직후, 연결 확인을 위한 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect") // 이벤트 이름
                    .data("SSE connection established. userId=" + userId)); // 데이터
        } catch (IOException e) {
            log.error("초기 연결 데이터 전송 실패: userId={}", userId, e);
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 알림을 보냅니다.
     * @param userId 알림을 받을 사용자의 ID
     * @param eventName 이벤트 이름 (프론트에서 구분하기 위함)
     * @param message 보낼 메시지
     */
    public void send(Long userId, String eventName, String message) {
        // 특정 사용자의 Emitter를 찾습니다.
        SseEmitter emitter = emitters.get(userId);

        // Emitter가 존재하면(사용자가 접속 중이면) 메시지를 보냅니다.
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(message));
                log.info("알림 발송 성공: userId={}, event={}, message={}", userId, eventName, message);
            } catch (IOException e) {
                // 메시지 전송 중 오류 발생 시 (예: 클라이언트 연결 끊김) Emitter를 제거합니다.
                emitters.remove(userId);
                log.error("알림 발송 실패, Emitter 제거: userId={}", userId, e);
            }
        } else {
            // Emitter가 없으면(사용자가 오프라인이면) 그냥 넘어갑니다.
            // 어차피 알림은 DB에 저장되어 있으므로, 나중에 로그인 시 확인할 수 있습니다.
            log.info("사용자가 오프라인 상태이므로 SSE 알림을 발송하지 않습니다: userId={}", userId);
        }
    }
}