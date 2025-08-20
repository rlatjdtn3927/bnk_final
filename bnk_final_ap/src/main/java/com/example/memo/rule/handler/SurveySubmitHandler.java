package com.example.memo.rule.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.rule.service.SurveyService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessage;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class SurveySubmitHandler implements TcpMessageHandler {

    private final SurveyService surveyService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        // 이 핸들러는 SURVEY_SUBMIT 요청만 처리
        return command == Command.SURVEY_SUBMIT;
    }

    @Override
    public TcpMessage handle(Command command, JsonNode data) {
        try {
            // 1) 요청 DTO 변환
            SurveySubmitReq req = objectMapper.convertValue(data, SurveySubmitReq.class);

            // 2) 점수 계산 및 DB 저장
            SurveySubmitRes result = surveyService.evaluateAndSave(req);

            // 3) 평문 JSON 생성 (암호화는 TcpClientService가 담당)
            String plainJson = objectMapper.writeValueAsString(
                Map.of("success", true, "data", result)
            );

            // 4) JSON 문자열을 다시 JsonNode로 변환
            JsonNode responseNode = objectMapper.readTree(plainJson);

            // 5) TcpMessage로 감싸서 반환
            return new TcpMessage(Command.SURVEY_SUBMIT, responseNode);

        } catch (Exception e) {
            try {
                // 에러 발생 시에도 평문 JSON 구조 유지
                String plainError = objectMapper.writeValueAsString(
                    Map.of("success", false, "message", e.getMessage())
                );
                JsonNode responseNode = objectMapper.readTree(plainError);
                return new TcpMessage(Command.SURVEY_SUBMIT, responseNode);
            } catch (Exception ex) {
                throw new RuntimeException("SurveySubmitHandler 처리 중 치명적 오류", ex);
            }
        }
    }
}
