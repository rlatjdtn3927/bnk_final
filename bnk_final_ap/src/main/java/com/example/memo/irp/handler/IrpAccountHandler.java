package com.example.memo.irp.handler;

import org.springframework.stereotype.Component;

import com.example.memo.irp.service.BankAccountService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class IrpAccountHandler implements TcpMessageHandler{
	
	private final BankAccountService bankAccountService;

	@Override
	public boolean supports(Command command) {
		return command.name().startsWith("IRP_ACCOUNT_");
	}

	@Override
	public Object handle(Command command, JsonNode data) {
		switch (command.name()) {
		
		}
		return null;
	}

}
