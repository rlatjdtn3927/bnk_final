package com.example.memo.ocr.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.ocr.dto.FamilyCertOcrRequest;
import com.example.memo.ocr.dto.FamilyCertOcrResponse;
import com.example.memo.ocr.service.FamilyCertOcrService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class FamilyCertOcrHandler implements TcpMessageHandler {

    @Autowired
    private FamilyCertOcrService familyCertOcrService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        // FAMILY_ 로 시작하는 모든 명령어 처리 가능
        return command.name().startsWith("FAMILY_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            switch (command) {
                case FAMILY_CERT_OCR: {
                    FamilyCertOcrRequest req =
                        objectMapper.convertValue(data, FamilyCertOcrRequest.class);
                    FamilyCertOcrResponse res =
                        familyCertOcrService.recognizeAndVerify(req);
                    return objectMapper.convertValue(res, JsonNode.class);
                }
                default:
                    return "알 수 없는 명령: " + command.name();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "처리 중 오류 발생: " + e.getMessage();
        }
    }
}