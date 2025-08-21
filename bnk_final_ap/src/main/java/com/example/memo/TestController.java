package com.example.memo;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.reserve.handler.BuyPlanHandler;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
	private final BuyPlanHandler buyPlanHandler;
	private final ObjectMapper mapper;
	
	@PostMapping("/testBuyPlan")
	public ResponseEntity<?> testBuyPlanUpdate(@RequestBody TcpMessage msg) throws JsonProcessingException {
		String response = mapper.writeValueAsString( buyPlanHandler.handle(msg.getCommand(), msg.getData()));
		
		return ResponseEntity.status(HttpStatus.OK).body(mapper.readTree(response));
	}
	
}
