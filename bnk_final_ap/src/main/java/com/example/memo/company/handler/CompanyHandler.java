package com.example.memo.company.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.company.dto.CompanyRegisterDto;
import com.example.memo.company.service.CompanyService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CompanyHandler implements TcpMessageHandler{
	
	@Autowired
	private CompanyService companyService;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Override
	public boolean supports(Command command) {
		// COMPANY_로 시작하는 모든 명령어 처리 가능
		return command.name().startsWith("COMPANY_");
	}
	
	@Override
	public Object handle(Command command, JsonNode data) {
	    try {
	        switch(command) {
	            case COMPANY_REGISTER:
	                CompanyRegisterDto dto = objectMapper.convertValue(data, CompanyRegisterDto.class);
	                return companyService.registerCompany(dto);
	            default:
	                return "알 수 없는 명령: " + command.name();
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "처리 중 오류 발생: " + e.getMessage();
	    }
	}

	

}
