package com.example.memo.rule.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.rule.service.ProfileResultService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessage;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ProfileResultHandler implements TcpMessageHandler {

    private final ProfileResultService profileResultService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        return command == Command.PROFILE_RESULT_GET;
    }

    @Override
    public TcpMessage handle(Command command, JsonNode data) {
        Long userId = data.path("userId").asLong();

        Map<String, Object> row = profileResultService.getProfileTypeByUserId(userId);

        JsonNode responseNode;
        if (row != null) {
            responseNode = objectMapper.valueToTree(
                Map.of("success", true,
                       "data", Map.of(
                           "typeId", row.get("type_id"),
                           "typeName", row.get("type_name")
                       ))
            );
        } else {
            responseNode = objectMapper.valueToTree(
                Map.of("success", false,
                       "error", "No profile result for userId=" + userId)
            );
        }

        return new TcpMessage(Command.PROFILE_RESULT_GET, responseNode);
    }
}
