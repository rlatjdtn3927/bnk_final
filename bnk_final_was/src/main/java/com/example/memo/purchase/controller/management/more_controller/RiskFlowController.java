package com.example.memo.purchase.controller.management.more_controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.example.memo.purchase.trade_common.dto.ReserveValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

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

        // AP 호출
        JsonNode req = om.createObjectNode().put("userId", userId);
        JsonNode res = tcp.sendMessage(new TcpMessage(Command.PROFILE_GET_LATEST_TYPE_BY_USER, req));

        String typeName = (res != null && res.hasNonNull("typeName")) ? res.get("typeName").asText() : null;
        Integer typeNo = (res != null && res.has("typeNo") && res.get("typeNo").isNumber())
                         ? res.get("typeNo").intValue() : null;

        // 세션 DTO 확인
        ReserveValueDto reserveDto = (ReserveValueDto) session.getAttribute("ReserveValueDto");
        ChangeValueDto changeDto   = (ChangeValueDto) session.getAttribute("ChangeValueDto");

        if (reserveDto != null) {
            reserveDto.setRiskGrade(typeName);
            reserveDto.setRiskGradeNum(typeNo);
            session.setAttribute("ReserveValueDto", reserveDto);
            return "redirect:/purchase/trade/reserve/step1/after-risk";
        } else if (changeDto != null) {
            changeDto.setRiskGrade(typeName);
            changeDto.setRiskGradeNum(typeNo);
            session.setAttribute("ChangeValueDto", changeDto);
            return "redirect:/purchase/trade/change/step1/after-risk";
        }

        // fallback: 엔트리로
        return "redirect:/purchase/products";
    }
}
