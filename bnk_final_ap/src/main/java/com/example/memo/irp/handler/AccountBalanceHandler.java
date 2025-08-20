package com.example.memo.irp.handler;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.memo.irp.service.BankAccountService;
import com.example.memo.irp.service.IrpJoinService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component("accountBalanceHandler")  // ✅ 고유한 빈 이름
@RequiredArgsConstructor
public class AccountBalanceHandler implements TcpMessageHandler{
	
	private final ObjectMapper mapper;
    private final BankAccountService bankAccountService;
    private final IrpJoinService irpJoinService; // joinId → userId 확인

	@Override
	public boolean supports(Command command) {
		return command.name().startsWith("ACCOUNT_GET_");
	}

	@Override
	public Object handle(Command command, JsonNode data) {
        switch (command) {
            case ACCOUNT_GET_BALANCE: {
                try {
                    Long joinId = requireLong(data, "joinId");
                    String acctNo = requireText(data, "acctNo");
                    // 비번은 옵션
                    String acctPwd = data.hasNonNull("acctPwd") ? data.get("acctPwd").asText() : null;

                    // joinId → userId
                    Long userId = irpJoinService.findByIdOrThrow(joinId).getUser().getUserId();

                    // 필요 시 엄격 검증 (비번까지 확인하려면 주석 해제)
                    // bankAccountService.verifyOrThrow(acctNo, userId, acctPwd);

                    // 사용자 소유 계좌일 때만 잔액 조회
                    BigDecimal bal = bankAccountService.getBalanceForUser(acctNo, userId);
                    long balance = (bal == null) ? 0L : bal.longValue();

                    ObjectNode res = mapper.createObjectNode();
                    res.put("ok", true);
                    res.put("balance", balance);
                    return res; // IrpJoinHandler와 동일하게 ObjectNode 반환

                } catch (Exception e) {
                    // IrpJoinHandler의 일부 케이스처럼 문자열 JSON fallback
                    ObjectNode err = mapper.createObjectNode();
                    err.put("ok", false);
                    err.put("error", e.getMessage() == null ? "balance inquiry failed" : e.getMessage());
                    try {
                        return mapper.writeValueAsString(err);
                    } catch (Exception ignore) {
                        return "{\"ok\":false,\"error\":\"serialize\"}";
                    }
                }
            }
            case ACCOUNT_GET_NUMBER: {  // ← 추가
                try {
                    Long joinId = requireLong(data, "joinId");
                    Long userId = irpJoinService.findByIdOrThrow(joinId).getUser().getUserId();

                    String acctNo = bankAccountService.getUserAccounts(userId).getAcctNo();

                    ObjectNode res = mapper.createObjectNode();
                    res.put("ok", true);
                    res.put("acctNo", acctNo);
                    return res;
                } catch (Exception e) {
                    ObjectNode err = mapper.createObjectNode();
                    err.put("ok", false);
                    err.put("error", e.getMessage() == null ? "account number inquiry failed" : e.getMessage());
                    try { return mapper.writeValueAsString(err); }
                    catch (Exception ignore) { return "{\"ok\":false,\"error\":\"serialize\"}"; }
                }
            }
            default:
                return "알 수 없는 명령: " + command.name();
        }
    }

	 // === IrpJoinHandler와 동일한 헬퍼들 ===
    private Long requireLong(JsonNode n, String field) {
        if (n.hasNonNull(field)) return n.get(field).asLong();
        throw new IllegalArgumentException("필수 필드 누락: " + field);
    }
    private String requireText(JsonNode n, String field) {
        if (n.hasNonNull(field)) return n.get(field).asText();
        throw new IllegalArgumentException("필수 필드 누락: " + field);
    }
}
