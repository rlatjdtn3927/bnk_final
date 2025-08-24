// src/main/java/com/example/memo/purchase/change/rest_controller/ChangeSubmitController.java
package com.example.memo.purchase.change.rest_controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.change.dto.RequestChangeDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/retain-api/change")
@RequiredArgsConstructor
public class ChangeSubmitController {

    private final ObjectMapper mapper;
    private final TcpClientService tcpClientService;

    /**
     * ✅ Step5 최종 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitChange(@RequestBody RequestChangeDto req, HttpSession session) {
        try {
            // 전체 DTO JSON 로그
            String jsonDebug = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req);
            System.out.println("== [/submit] 요청 DTO 전체(JSON):\n" + jsonDebug);

            // 개별 필드 확인
            System.out.println("== accountType: " + req.getAccountType());
            System.out.println("== accountId: " + req.getAccountId());

            // 매도 펀드
            if (req.getSoldProdList() != null) {
                System.out.println("== 매도 펀드 목록 (" + req.getSoldProdList().size() + "개)");
                req.getSoldProdList().forEach(f ->
                    System.out.println("   - prodId=" + f.getProdId() + ", ratio=" + f.getRatio())
                );
            } else {
                System.out.println("== 매도 펀드 목록 없음");
            }

            // 매도 예금
            if (req.getSoldPrincipalIdList() != null) {
                System.out.println("== 매도 예금 목록 (" + req.getSoldPrincipalIdList().size() + "개)");
                req.getSoldPrincipalIdList().forEach(p ->
                    System.out.println("   - id=" + p.getId() + ", ratio=" + p.getRatio())
                );
            } else {
                System.out.println("== 매도 예금 목록 없음");
            }

            // 매수 펀드
            if (req.getBuyFundList() != null) {
                System.out.println("== 매수 펀드 목록 (" + req.getBuyFundList().size() + "개)");
                req.getBuyFundList().forEach(b ->
                    System.out.println("   - prodId=" + b.getProdId() + ", cost=" + b.getCost())
                );
            } else {
                System.out.println("== 매수 펀드 목록 없음");
            }

            // 매수 예금
            if (req.getBuyPrincipalList() != null) {
                System.out.println("== 매수 예금 목록 (" + req.getBuyPrincipalList().size() + "개)");
                req.getBuyPrincipalList().forEach(b ->
                    System.out.println("   - prodId=" + b.getProdId() + ", cost=" + b.getCost())
                );
            } else {
                System.out.println("== 매수 예금 목록 없음");
            }

            // AP로 그대로 전송
            JsonNode payload = mapper.valueToTree(req);
            TcpMessage msg = new TcpMessage(Command.CHANGE_UPDATE_LEDGER, payload);
            JsonNode response = tcpClientService.sendMessage(msg);

            System.out.println("== [/submit] AP 응답:\n" + response.toPrettyString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                java.util.Map.of("ok", false, "error", e.getMessage())
            );
        }
    }
}
