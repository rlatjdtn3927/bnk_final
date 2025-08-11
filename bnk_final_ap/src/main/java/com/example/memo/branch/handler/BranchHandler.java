package com.example.memo.branch.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.memo.branch.service.BranchService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BranchHandler implements TcpMessageHandler {

    private final BranchService branchService;

    @Override
    public boolean supports(Command command) {
        return command == Command.BRANCH_NEARBY;
    }

    @Override
    public Object handle(Command command, JsonNode data) {
        try {
        	//위도 추출
            double lat = data.path("latitude").asDouble();
            //경도 추출
            double lon = data.path("longitude").asDouble();
            //검색 반경 추출(있으면 있는대로 없으면 기본값 2.0km
            double radiusKm = data.has("radiusKm") && !data.get("radiusKm").isNull()
                    ? data.path("radiusKm").asDouble()
                    : 2.0;
            //지점 검색 실행
            return branchService.findNearbyBusanBank(lat, lon, radiusKm);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
