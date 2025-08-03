package com.example.memo.tcp_common;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.memo.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TcpMessageRouter {
		
	@Autowired
	private ObjectMapper mapper;
	
	private final Map<Command, TcpMessageHandler> handlerMap = new EnumMap<>(Command.class);
	
	@Autowired
    public TcpMessageRouter(List<TcpMessageHandler> handlers) {
		for(TcpMessageHandler handler : handlers) {
			for(Command command : Command.values()) {
				if(handler.supports(command)) {
					handlerMap.put(command, handler);
				}
			}
		}
    }
	
	public String route(TcpMessage msg) throws JsonProcessingException {
		Command command = msg.getCommand();
		TcpMessageHandler handler = handlerMap.get(command);
		if(handler == null) {
			System.out.println("정의 되지 않은 프로토콜입니다.");
		}
		Object result = handler.handle(command, msg.getData()); 
		return mapper.writeValueAsString(result);
	}
	
}
