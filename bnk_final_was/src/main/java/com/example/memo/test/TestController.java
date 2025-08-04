package com.example.memo.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/*실제로 보내는 예시 참고 하세요*/
@Controller
public class TestController {
	
	@Autowired
	private TcpClientService tcpService;
	@Autowired
	private ObjectMapper objectMapper;
	
	@PostMapping("/tcpCommunication")
	@ResponseBody
	public ResponseEntity<?> testCommunication(@RequestBody TestUserDto userDto) {
		Command cmd = Command.USER_GET;
		JsonNode jsonNode = objectMapper.convertValue(userDto, JsonNode.class);
		TcpMessage msg = new TcpMessage(cmd, jsonNode);
		return ResponseEntity.status(HttpStatus.OK).body(tcpService.sendMessage(msg));
	}

}
