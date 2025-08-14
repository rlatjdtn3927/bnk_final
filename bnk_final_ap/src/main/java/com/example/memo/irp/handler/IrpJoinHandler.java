package com.example.memo.irp.handler;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.memo.irp.dto.AccountContractResult;
import com.example.memo.irp.service.IrpJoinService;
import com.example.memo.jpa.entity.irp.IrpJoinEntity;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpMessageHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
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
			case IRP_JOIN_GET: {
			    Long joinId = data.get("joinId").asLong();
			    IrpJoinEntity j = joinService.findByIdOrThrow(joinId); // 서비스에 간단히 추가
			    
			    ObjectNode res = mapper.createObjectNode();
			    res.put("joinId", j.getJoinId());
			    res.put("joinPurpose", j.getJoinPurpose());
			    res.put("contractNo", j.getContractNo());
			    res.put("annualContribAmt", j.getAnnualContribAmt());
			    res.put("newContribAmt", j.getNewContribAmt());
			    res.put("branchOffice", j.getBranchOffice());
			    if (j.getIrpAccount()!=null) {
			        res.put("irpAcctNo", j.getIrpAccount().getIrpAcctNo());
			    }
			    return res;
			}
			case IRP_JOIN_SAVE:
				IrpJoinEntity joinEntity = mapper.convertValue(data, IrpJoinEntity.class);
				return joinService.saveJoin(joinEntity);
			case IRP_JOIN_TAX_PURPOSE: {
				Long joinId = requireLong(data, "joinId");
			    String qualType = requireText(data, "irpQualType"); // "근로자"/"자영업자"
				
				// 2) 옵션 파라미터(없으면 null로)
			    String busiNo = null;
			    JsonNode bn = data.get("businessNo");
			    if (bn != null && !bn.isNull()) {
			        String v = bn.asText();
			        if (v != null && !v.isBlank()) {
			            busiNo = v;
			        }
			    }
				joinService.saveTaxPurpose(joinId, qualType, busiNo);
				ObjectNode res = mapper.createObjectNode();
				 res.put("result", "OK");
				 res.put("joinId", joinId);
				return res;
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
                ObjectNode res = mapper.createObjectNode().put("joinId", joinId);
                return res;
            }
			case IRP_JOIN_UPDATE_CONTRACT: {	// step3-2: 계약정보 업데이트
                Long joinId = data.get("joinId").asLong();
                Long annualAmt = data.path("annualContribAmt").asLong(0);
                Long newAmt    = data.path("newContribAmt").asLong(0);
                String acctNo  = data.get("acctNo").asText();
                String acctPwd = data.get("acctPwd").asText();
                String branch  = data.path("branchOffice").asText();
                
                joinService.updateContract(joinId, annualAmt, newAmt, acctNo, acctPwd, branch);
                ObjectNode res = mapper.createObjectNode();
                res.put("result", "OK");
                res.put("joinId", joinId);
                res.put("updated", 1);
                
                try {
                    return mapper.writeValueAsString(res);
                } catch (JsonProcessingException e) {
                    // 직렬화 실패 시 fallback
                    return "{\"result\":\"ERROR\",\"message\":\"Serialization failed\"}";
                }
            }
			case IRP_JOIN_UPDATE_PRODUCT: {		// step4: 상품 저장
                Long joinId = data.get("joinId").asLong();
                String productId = data.get("productId").asText();
                joinService.updateJoinProduct(joinId, productId);
                return mapper.createObjectNode().put("result", "OK");
            }
			case IRP_JOIN_COMPLETE: { // 가입완료 → IRP계좌 생성
                Long joinId = data.get("joinId").asLong();
                String irpPwd = data.get("irpPwd").asText();
                
                // 서비스가 계좌번호와 계약번호를 돌려주도록 구성 가능
                //String irpAcctNo = joinService.completeJoinAndOpenIrpAccount(joinId, irpPwd);
                AccountContractResult result = joinService.completeJoinAndOpenIrpAccount(joinId, irpPwd);
                
                ObjectNode res = mapper.createObjectNode();
                res.put("irpAcctNo", result.getIrpAcctNo());
                res.put("contractNo", result.getContractNo());
                return res;
            }
			default:
				return "알 수 없는 명령: " + command.name();
		}
	}
	
	private Long requireLong(JsonNode n, String field) {
	    if (n.hasNonNull(field)) return n.get(field).asLong();
	    throw new IllegalArgumentException("필수 필드 누락: " + field);
	}
	private String requireText(JsonNode n, String field) {
	    if (n.hasNonNull(field)) return n.get(field).asText();
	    throw new IllegalArgumentException("필수 필드 누락: " + field);
	}
	
}
