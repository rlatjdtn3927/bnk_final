package com.example.memo.auth.handler;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.example.memo.auth.dto.ApiResult;
import com.example.memo.auth.dto.UserLoginResultDTO;
import com.example.memo.auth.service.AuthService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class UserLoginHandler implements TcpMessageHandler {

    private final AuthService authService;

    @Override
    public boolean supports(Command command) {
        return command == Command.USER_LOGIN;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            // 1) 우선 username 기반
            String username = textOrNull(data, "username");
            String password = textOrNull(data, "password");

            // 2) 필요 시 userId 기반도 허용 (주석 해제)
            // Integer userId = data.hasNonNull("userId") ? data.get("userId").asInt() : null;

            if (isBlank(username) || isBlank(password)) {
                // if (userId == null || isBlank(password)) ...
                return ApiResult.fail("BAD_REQUEST", "username/password 누락");
            }

            UserLoginResultDTO result = authService.loginUserByUsername(username, password);
            // userId 기반일 경우: authService.loginUserByUserId(userId, password);

            return ApiResult.ok(result);

        } catch (IllegalArgumentException e) {
            if ("INVALID_CREDENTIALS".equals(e.getMessage())) {
                return ApiResult.fail("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            return ApiResult.fail("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            return ApiResult.fail("INTERNAL_ERROR", "시스템 오류");
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
