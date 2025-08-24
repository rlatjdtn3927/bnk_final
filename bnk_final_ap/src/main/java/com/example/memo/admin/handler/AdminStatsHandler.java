package com.example.memo.admin.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.admin.service.AdminStatsAggService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminStatsHandler implements TcpMessageHandler {

    private final AdminStatsAggService stats;

    @Override
    public boolean supports(Command command) {
        return command.name().startsWith("ADMIN_STATS_");
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        var fullStats = stats.buildAdminStats();

        return switch (command.name()) {
            case "ADMIN_STATS_OVERVIEW"   -> Map.of("result", fullStats);
            case "ADMIN_STATS_TOTAL"      -> Map.of("result", fullStats.totalAssetsByType());
            case "ADMIN_STATS_ALLOCATION" -> Map.of("result", fullStats.allocation());
            case "ADMIN_STATS_RISK"       -> Map.of("result", fullStats.riskSplit());
            default -> Map.of("error", "Unsupported command");
        };
    }
}