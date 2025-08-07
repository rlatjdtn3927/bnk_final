package com.example.memo.company.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.company.service.SubscriberService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SubscriberHandler implements TcpMessageHandler {

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(Command command) {
        // SUBSCRIBER_로 시작하는 모든 명령어 처리
        return command.name().startsWith("SUBSCRIBER_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
        	switch (command) {
	            case SUBSCRIBER_VALIDATE:
	                return subscriberService.validateSubscribers(data);
	            case SUBSCRIBER_REGISTER:
	                return subscriberService.dcMemberRegister(data);
	            default:
	                return "알 수 없는 명령: " + command.name();
	        }
        } catch (Exception e) {
            e.printStackTrace();
            return "처리 중 오류 발생: " + e.getMessage();
        }
    }
}
