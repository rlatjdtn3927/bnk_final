package com.example.memo.auth.handler;

import org.springframework.stereotype.Component;

import com.example.memo.auth.dto.ApiResult;
import com.example.memo.auth.dto.UserRegisterDTO;
import com.example.memo.auth.service.UserService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRegisterHandler implements TcpMessageHandler {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        return command == Command.USER_REGISTER;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            UserRegisterDTO dto = objectMapper.treeToValue(data, UserRegisterDTO.class);

            if (isBlank(dto.getUsername()) || isBlank(dto.getPassword()) ||
                isBlank(dto.getEmail()) || isBlank(dto.getGender()) ||
                dto.getBirthDate() == null || isBlank(dto.getRrn())) {
                return ApiResult.fail("BAD_REQUEST", "필수값 누락");
            }

            Long userId = userService.register(dto);
            return ApiResult.ok(java.util.Map.of(
                "userId", userId,
                "username", dto.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            String m = e.getMessage();
            if ("DUPLICATE_USERNAME".equals(m)) return ApiResult.fail("DUPLICATE_USERNAME", "이미 사용 중인 아이디입니다.");
            if ("DUPLICATE_EMAIL".equals(m))    return ApiResult.fail("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
            return ApiResult.fail("BAD_REQUEST", "요청 값을 확인해 주세요.");
        } catch (Exception e) {
            return ApiResult.fail("INTERNAL_ERROR", "시스템 오류");
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
