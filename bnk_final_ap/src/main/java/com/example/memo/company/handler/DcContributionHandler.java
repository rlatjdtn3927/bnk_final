package com.example.memo.company.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.company.dto.ContribValidationResultDto;
import com.example.memo.company.service.DcContributionService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DcContributionHandler implements TcpMessageHandler {

    private final DcContributionService service;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("CONTRIBUTION_BATCH_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case CONTRIBUTION_BATCH_CREATE: {
                    Long batchId = service.createContributionBatch(data);
                    return Map.of("batchId", batchId);
                }
                case CONTRIBUTION_BATCH_VALIDATE: {
                    Long batchId = data.get("batchId").asLong();
                    ContribValidationResultDto result = service.validateBatch(batchId);
                    return objectMapper.convertValue(result, JsonNode.class);
                }
                case CONTRIBUTION_BATCH_CONFIRM: {
                    Long batchId = data.get("batchId").asLong();
                    Long companyId = data.get("companyId").asLong(); // WAS에서 세션 회사ID 전달
                    Map<String, Object> result = service.confirmBatch(batchId, companyId);
                    return objectMapper.convertValue(result, JsonNode.class);
                }
                default:
                    return Map.of("error", "Unsupported command");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}