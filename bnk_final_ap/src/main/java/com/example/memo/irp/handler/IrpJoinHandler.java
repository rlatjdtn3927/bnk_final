package com.example.memo.irp.handler;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.memo.irp.entity.IrpJoinEntity;
import com.example.memo.irp.service.IrpJoinService;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IrpJoinHandler implements TcpMessageHandler {
	
	private final IrpJoinService joinService;
	private final ObjectMapper mapper;
	
	@Override
	public boolean supports(Command command) {
		return command.name().startsWith("IRP_JOIN_");
	}

	@Override
	public Object handle(Command command, JsonNode data) {
		switch (command) {
			case IRP_JOIN_SAVE:
				IrpJoinEntity joinEntity = mapper.convertValue(data, IrpJoinEntity.class);
				return joinService.saveJoin(joinEntity);
			case IRP_JOIN_TAX_PURPOSE: {
				Long joinId = data.get("joinId").asLong();
				String qualType = data.get("irpQualType").asText();
				String busiNo = data.get("businessNo").asText();
				joinService.saveTaxPurpose(joinId, qualType, busiNo);
				return "OK";
			}
			case IRP_JOIN_RETIRED_PURPOSE: {
	            Long joinId = data.get("joinId").asLong();
	            LocalDate retireDate = LocalDate.parse(data.get("retireDate").asText());
	            String reason = data.path("retireReason").asText();
	            String corpName = data.get("corpName").asText();
	            Long amt = data.path("severanceAmt").asLong(0);
	            String doc = data.get("withholdDoc").asText();
	            joinService.saveRetirePurpose(joinId, retireDate, reason, corpName, amt, doc);
	            return "OK";
			}
			case IRP_JOIN_CREATE_DRAFT: {	// step1: 초안 생성
                Long userId = data.get("userId").asLong();
                String purpose = data.get("joinPurpose").asText();
                Long joinId = joinService.createDraft(userId, purpose);
                return mapper.createObjectNode().put("joinId", joinId);
            }
			case IRP_JOIN_UPDATE_CONTRACT: {	// step3-2: 계약정보 업데이트
                Long joinId = data.get("joinId").asLong();
                Long annual = data.path("annualContribAmt").asLong(0);
                Long news   = data.path("newContribAmt").asLong(0);
                //String contractNo = data.get("contractNo").asText();
                String acctNo     = data.get("acctNo").asText();
                String acctPwd = data.get("acctPwd").asText();
                String branch     = data.path("branchOffice").asText();
                joinService.updateContract(joinId, annual, news, /*contractNo,*/ acctNo, branch, acctPwd);
                return "OK";
            }
			case IRP_JOIN_UPDATE_PRODUCT: {		// step4: 상품 저장
                Long joinId = data.get("joinId").asLong();
                String productId = data.get("productId").asText();
                joinService.updateJoinProduct(joinId, productId);
                return "OK";
            }
			case IRP_JOIN_COMPLETE: { // 가입완료 → IRP계좌 생성
                Long joinId = data.get("joinId").asLong();
                String irpPwd = data.get("irpPwd").asText();
                String newAcctNo = joinService.completeJoinAndOpenIrpAccount(joinId, irpPwd);
                ObjectNode res = mapper.createObjectNode();
                res.put("irpAcctNo", newAcctNo);
                return res;
            }
			default:
				return "알 수 없는 명령: " + command.name();
		}
	}
	
}
