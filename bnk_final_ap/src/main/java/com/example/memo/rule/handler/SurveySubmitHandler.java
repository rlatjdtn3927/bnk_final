package com.example.memo.rule.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.rule.dto.SurveySubmitReq;
import com.example.memo.rule.dto.SurveySubmitRes;
import com.example.memo.rule.service.SurveyService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessage;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.example.memo.tcp_common.AES256Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class SurveySubmitHandler implements TcpMessageHandler {

    private final SurveyService surveyService;
    private final ObjectMapper objectMapper;
    private final AES256Util AES256Util;

    @Override
    public boolean supports(Command command) {
        return command == Command.SURVEY_SUBMIT;
    }

    @Override
    public TcpMessage handle(Command command, JsonNode data) {
        try {
            // 1. 요청 DTO 변환
            SurveySubmitReq req = objectMapper.convertValue(data, SurveySubmitReq.class);

            // 2. 점수 계산 및 DB 저장
            SurveySubmitRes result = surveyService.evaluateAndSave(req);

            // 3. WAS가 기대하는 평문 JSON 구조를 먼저 만듭니다.
            Map<String, Object> plainResponseMap = Map.of("success", true, "data", result);
            String plainJson = objectMapper.writeValueAsString(plainResponseMap);

            // 4. 평문 JSON을 암호화합니다.
            String encData = AES256Util.encrypt(plainJson);

            // 5. 암호화된 문자열을 encData 키에 담아 최종 응답 JSON을 생성합니다.
            JsonNode responseNode = objectMapper.valueToTree(
                Map.of("encData", encData)
            );

            // 6. TcpMessage로 감싸서 반환합니다.
            return new TcpMessage(Command.SURVEY_SUBMIT, responseNode);

        } catch (Exception e) {
            try {
                // 에러 발생 시에도 암호화된 응답 구조를 유지합니다.
                String plainError = objectMapper.writeValueAsString(
                    Map.of("success", false, "message", "AP 서버 처리 오류: " + e.getMessage())
                );
                String encError = AES256Util.encrypt(plainError);
                JsonNode errorNode = objectMapper.valueToTree(Map.of("encData", encError));
                
                return new TcpMessage(Command.SURVEY_SUBMIT, errorNode);
                
            } catch (Exception ex) {
                // 이중 예외 발생 시 치명적 오류 처리
                throw new RuntimeException("SurveySubmitHandler 처리 중 치명적 오류", ex);
            }
        }
    }
}