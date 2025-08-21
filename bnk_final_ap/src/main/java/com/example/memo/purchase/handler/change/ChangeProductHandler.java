package com.example.memo.purchase.handler.change;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

public class ChangeProductHandler implements TcpMessageHandler{

	@Override
	public boolean supports(Command command) {
		return command.name().startsWith("CHANGE_");
	}

	@Override
	public Object handle(Command command, JsonNode data) {
		
		switch(command.name()) {
		case "CHANGE_UPDATE_PRODUCT": {
			break;
		}
		case "CHANGE_GET_HOLDINGS": {
			break;
		}
		}
		return null;
	}

}
