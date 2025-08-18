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
        SurveySubmitReq req = objectMapper.convertValue(data, SurveySubmitReq.class);

        SurveySubmitRes result = surveyService.evaluateAndSave(req);

        // 응답 JSON을 { success:true, data: {...} } 형태로
        JsonNode responseNode = objectMapper.valueToTree(
            Map.of("success", true, "data", result)
        );

        return new TcpMessage(Command.SURVEY_SUBMIT, responseNode);
    }

}
