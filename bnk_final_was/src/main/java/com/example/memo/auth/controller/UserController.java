package com.example.memo.auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.memo.auth.dto.UserJoinDTO;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user-api")
public class UserController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/register", produces = "application/json")
    public ResponseEntity<?> register(@RequestBody UserJoinDTO dto) {
        Map<String, Object> body = new HashMap<>();
        try {
            if (isBlank(dto.getUsername()) || isBlank(dto.getPassword())
                    || isBlank(dto.getEmail()) || isBlank(dto.getGender())
                    || isBlank(dto.getJob()) || isBlank(dto.getBirthDate())
                    || isBlank(dto.getRrn())) {
                body.put("ok", false);
                body.put("success", false);
                body.put("code", "BAD_REQUEST");
                body.put("message", "필수 항목을 모두 입력해 주세요.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            JsonNode data = objectMapper.valueToTree(dto);
            TcpMessage msg = new TcpMessage(Command.USER_REGISTER, data);
            JsonNode root = tcpClientService.sendMessage(msg);

            if (root == null) {
                body.put("ok", false);
                body.put("success", false);
                body.put("code", "AP_UNREACHABLE");
                body.put("message", "AP 응답 없음");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
            }

            boolean success = root.path("success").asBoolean(false);
            String apCode = root.path("code").asText("");
            String apMessage = root.path("message").asText("");

            if (!success) {
                HttpStatus st = switch (apCode) {
                    case "DUPLICATE_USERNAME", "DUPLICATE_EMAIL" -> HttpStatus.CONFLICT;
                    case "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
                    default -> HttpStatus.BAD_GATEWAY;
                };
                body.put("ok", false);
                body.put("success", false);
                body.put("code", apCode);
                body.put("message", isBlank(apMessage) ? "회원가입 실패" : apMessage);
                return ResponseEntity.status(st).body(body);
            }

            body.put("ok", true);
            body.put("success", true);
            body.put("code", "OK");
            body.put("message", "회원가입 성공");
            body.put("data", root.path("data"));
            body.put("redirect", "/login-view");
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            e.printStackTrace();
            body.put("ok", false);
            body.put("success", false);
            body.put("code", "INTERNAL_ERROR");
            body.put("message", "시스템 오류");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
