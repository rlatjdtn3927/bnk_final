package com.example.memo.company.handler;

import com.example.memo.company.dto.CompanyLoginRequestDto;
import com.example.memo.company.dto.CompanyLoginResponseDto;
import com.example.memo.company.service.CompanyService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CompanyLoginHandler implements TcpMessageHandler {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        return command == Command.COMPANY_LOGIN;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
            // JSON → DTO 변환
            CompanyLoginRequestDto loginDto = objectMapper.convertValue(data, CompanyLoginRequestDto.class);

            // 로그인 서비스 호출
            CompanyLoginResponseDto response = companyService.login(loginDto);

            // DTO → JSON 변환 후 반환
            return objectMapper.convertValue(response, JsonNode.class);

        } catch (Exception e) {
            e.printStackTrace();
            return objectMapper.convertValue("처리 중 오류 발생: " + e.getMessage(), JsonNode.class);
        }
    }
}
