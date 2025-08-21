package com.example.memo.admin.handler;

import org.springframework.stereotype.Component;

import com.example.memo.admin.service.ReportService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportMonthlyHandler implements TcpMessageHandler{
	
	private final ReportService reportService;

	@Override
	public boolean supports(Command command) {
		return command == Command.REPORT_MONTHLY_SUBSCRIBERS;
	}

	@Override
	public Object handle(Command command, JsonNode data) {
		// 요청 JSON 파라미터 (예: {"fromYm":"2025-01","toYm":"2025-08"})
        String fromYm = data.path("fromYm").asText();
        String toYm   = data.path("toYm").asText();

        // Service 호출 → DTO 반환
        return reportService.getMonthlySeries(fromYm, toYm);
	}

}
