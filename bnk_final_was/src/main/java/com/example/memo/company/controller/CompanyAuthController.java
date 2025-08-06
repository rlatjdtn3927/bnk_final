package com.example.memo.company.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.company.dto.CompanyLoginRequestDto;
import com.example.memo.company.dto.CompanyLoginResponseDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/company/auth")
public class CompanyAuthController {

	@Autowired
	private TcpClientService tcpService;

	@Autowired
	private ObjectMapper objectMapper;

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody CompanyLoginRequestDto loginDto, HttpServletRequest request) {
		try {
			Command cmd = Command.COMPANY_LOGIN;
			JsonNode data = objectMapper.convertValue(loginDto, JsonNode.class);
			TcpMessage msg = new TcpMessage(cmd, data);

			Object result = tcpService.sendMessage(msg);

			System.out.println("result class: " + result.getClass().getName());
			System.out.println("result content: " + result.toString());

			// 실패 응답: 문자열이면 바로 에러 처리
			if (result instanceof String) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
			}

			if (result instanceof JsonNode && ((JsonNode) result).isTextual()) {
				String errorMessage = ((JsonNode) result).asText();
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
			}

			// 성공 응답: CompanyLoginResponseDto로 파싱
			CompanyLoginResponseDto loginUser = objectMapper.convertValue(result, CompanyLoginResponseDto.class);

			// 세션 저장
			request.getSession().setAttribute("loginManager", loginUser);

			return ResponseEntity.ok("로그인 성공");

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("로그인 처리 중 오류: " + e.getMessage());
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpServletRequest request) {
		request.getSession().invalidate();
		return ResponseEntity.ok("로그아웃 성공");
	}
}
