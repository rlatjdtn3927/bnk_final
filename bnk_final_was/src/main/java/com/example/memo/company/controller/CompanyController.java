package com.example.memo.company.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.company.dto.CompanyRegisterDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/company")
public class CompanyController {
	
	@Autowired
	private TcpClientService tcpService;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@PostMapping("/register")
	public ResponseEntity<?> registerCompany(@RequestBody CompanyRegisterDto companyRegisterDto){
		
		// 1. Command 설정
		Command cmd = Command.COMPANY_REGISTER;
		
		// 2. DTO를 JsonNode로 변환
		JsonNode data = objectMapper.convertValue(companyRegisterDto, JsonNode.class);
	
		// 3. TcpMessage 생성
		TcpMessage msg = new TcpMessage(cmd, data);
		
		// 4. TCP 전송 후 응답 받기
		Object response = tcpService.sendMessage(msg);
		
		// 5. 클라이언트에게 응답 반환
		return ResponseEntity.ok(response);
	}

}
