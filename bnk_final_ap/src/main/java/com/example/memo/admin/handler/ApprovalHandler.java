package com.example.memo.admin.handler;

import org.springframework.stereotype.Component;

import com.example.memo.admin.service.ApprovalService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalHandler implements TcpMessageHandler{
	
	private final ApprovalService approvalService;

	@Override
	public boolean supports(Command command) {
		return command.name().startsWith("ADMIN_CRAWL_");
	}

	@Override
	public Object handle(Command command, JsonNode data) {
		
		switch(command) {
			case ADMIN_CRAWL_CHECK_UPDATE: {
				return approvalService.findUpdate();
			}
			case ADMIN_CRAWL_CHECK_FILE_TASK: {
				return approvalService.findAllFileTask();
			}
			case ADMIN_CRAWL_GRANT_FILE_DOWNLOAD: {
				return approvalService.downloadFiles(data);
			}
			default: {
				return null;
			}
		}
	}
	
}
