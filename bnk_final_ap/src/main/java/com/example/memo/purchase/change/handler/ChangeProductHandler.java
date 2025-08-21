package com.example.memo.purchase.change.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.purchase.change.dto.ResponseAccountHoldingsDto;
import com.example.memo.purchase.change.service.ChangeProductService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChangeProductHandler implements TcpMessageHandler{

	private final ChangeProductService changeProductService;
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
				ResponseAccountHoldingsDto result = changeProductService.getHoldings(data);
				if(result != null) {
					return Map.of("resultList", result); /*조회 성공 하면 이거 반환*/
				} else {
					return Map.of("error", "보유현황 조회 실패"); /*조회 실패 하면 이거 반환*/
				}
			}
		}
		return Map.of("error", "핸들러 응답 실패"); /*조회 실패 하면 이거 반환*/
	}

}
