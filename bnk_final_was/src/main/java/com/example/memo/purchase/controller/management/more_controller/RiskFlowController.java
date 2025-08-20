package com.example.memo.purchase.controller.management.more_controller;

import com.example.memo.purchase.dto.trade.PassValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class RiskFlowController {

    private final TcpClientService tcp;
    private final ObjectMapper om;

    /** 설문결과에서 "확인" → 최신 투자성향 조회 → PassValueDto.riskGrade(+Num) 세팅 → step2 이동 */
    @GetMapping("/purchase/trade/getProfileType")
    public String getProfileType(HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) throw new IllegalStateException("로그인이 필요합니다.");

        JsonNode req = om.createObjectNode().put("userId", userId);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PROFILE_GET_LATEST_TYPE_BY_USER, req));

        String typeName = (res != null && res.hasNonNull("typeName")) ? res.get("typeName").asText() : null;
        Integer typeNo = null;
        if (res != null && res.has("typeNo") && !res.get("typeNo").isNull() && res.get("typeNo").isNumber()) {
            typeNo = res.get("typeNo").intValue(); // 현재는 typeId를 넘김
        }

        PassValueDto dto = (PassValueDto) session.getAttribute("PassValueDto");
        if (dto == null) dto = new PassValueDto(); // 방어
        dto.setRiskGrade(typeName);
        dto.setRiskGradeNum(typeNo);
        session.setAttribute("PassValueDto", dto);

        return "redirect:/purchase/trade/step2/after-risk";
    }
}
