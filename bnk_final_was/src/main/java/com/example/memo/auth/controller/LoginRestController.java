package com.example.memo.auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.memo.auth.dto.LoginDTO;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/login-api")
public class LoginRestController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDto,
                                   HttpServletRequest request,
                                   HttpSession session) {
        Map<String, Object> body = new HashMap<>();
        try {
            // 0) 파라미터 검증
            String usernameReq = loginDto != null ? loginDto.getUsername() : null;
            String passwordReq = loginDto != null ? loginDto.getPassword() : null;
            if (isBlank(usernameReq) || isBlank(passwordReq)) {
                body.put("success", false);
                body.put("code", "BAD_REQUEST");
                body.put("message", "아이디/비밀번호를 입력하세요.");
                body.put("data", null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            // 1) AP 호출
            JsonNode data = objectMapper.valueToTree(loginDto);
            TcpMessage msg = new TcpMessage(Command.USER_LOGIN, data);
            JsonNode root = tcpClientService.sendMessage(msg);

            System.out.println("=== AP 응답 ===");
            System.out.println(root);

            if (root == null) {
                body.put("success", false);
                body.put("code", "AP_UNREACHABLE");
                body.put("message", "AP 응답 없음");
                body.put("data", null);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
            }

            boolean success = root.path("success").asBoolean(false);
            String apCode = root.path("code").asText("");
            String apMessage = root.path("message").asText("");

            if (!success) {
                body.put("success", false);
                body.put("code", isBlank(apCode) ? "ERROR" : apCode);
                body.put("message", isBlank(apMessage) ? "로그인 실패" : apMessage);
                body.put("data", null);
                HttpStatus status = "INVALID_CREDENTIALS".equals(apCode) 
                        ? HttpStatus.UNAUTHORIZED 
                        : HttpStatus.BAD_GATEWAY;
                return ResponseEntity.status(status).body(body);
            }

            // 2) 성공 처리
            JsonNode d = root.path("data");
            long userId = d.path("userId").asLong(-1);
            String username = d.path("username").asText(null);
            String displayName = d.path("displayName").asText(null); // ✅ 추가
            
            if (userId <= 0) {
                body.put("success", false);
                body.put("code", "BAD_AP_RESPONSE");
                body.put("message", "AP 데이터 형식이 올바르지 않습니다.");
                body.put("data", null);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
            }

            // 세션 고정화 방지
            request.changeSessionId();

            // ✅ 세션 저장 (설문 컨트롤러와 통일)
            session.setAttribute("LOGIN_USER_ID", userId); // 사장님 이거 뭐에요
            session.setAttribute("username", username); // 얘도 ???
            session.setAttribute("displayName", displayName); // 실제 이름(별칭)
            session.setAttribute("user", userId);


            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("username", username);
            payload.put("displayName", displayName); // ✅ 함께 내려주기

            body.put("success", true);
            body.put("code", "OK");
            body.put("message", "로그인 성공");
            body.put("data", payload);
            body.put("redirect", "/");

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("code", "INTERNAL_ERROR");
            err.put("message", "시스템 오류");
            err.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", "OK");
        body.put("message", "로그아웃");
        body.put("data", null);
        return ResponseEntity.ok(body);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
