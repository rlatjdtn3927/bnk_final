package com.example.memo.irp.handler;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.memo.irp.dto.AccountContractResult;
import com.example.memo.irp.dto.InitOpenResponse;
import com.example.memo.irp.dto.JoinSummaryResult;
import com.example.memo.irp.service.IrpJoinService;
import com.example.memo.jpa.entity.irp.IrpJoinEntity;
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
			case IRP_JOIN_UPDATE_CONTRACT: {	// step3: 계약정보 업데이트
				try {
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
					return res;
					
				} catch(IllegalArgumentException | SecurityException e) {
					// 비밀번호 불일치/잔액부족 등 비즈니스 실패 (다음 페이지로 넘어가면 안 됨)
			        ObjectNode err = mapper.createObjectNode();
			        err.put("ok", false);
			        err.put("error", e.getMessage() == null ? "요청이 거부되었습니다." : e.getMessage());
			        return err;
				} 
            }
			case IRP_JOIN_GET_CONTRACT: { // step3: 계약정보 조회 (폼 채우기)
			    try {
			        Long joinId = data.get("joinId").asLong();
			        JoinSummaryResult s = joinService.getContract(joinId);

			        ObjectNode res = mapper.createObjectNode();
			        res.put("ok", true);
			        res.put("joinId", s.getJoinId());
			        if (s.getBranchOffice() != null)     res.put("branchOffice", s.getBranchOffice());
			        if (s.getAnnualContribAmt() != null) res.put("annualContribAmt", s.getAnnualContribAmt());
			        if (s.getNewContribAmt() != null)    res.put("newContribAmt", s.getNewContribAmt());
			        return res;

			    } catch (IllegalArgumentException e) {
			        ObjectNode err = mapper.createObjectNode();
			        err.put("ok", false);
			        err.put("error", e.getMessage() == null ? "NOT_FOUND" : e.getMessage());
			        return err;
			    }
			}
			case IRP_JOIN_PREPARE_OPEN: { // Step4 요약 조회
			    Long joinId = data.get("joinId").asLong();
			    // 서비스는 요약 DTO를 돌려줌 (Long 타입 금액 사용)
		        JoinSummaryResult s = joinService.prepareOpen(joinId);

		        ObjectNode res = mapper.createObjectNode();
		        res.put("ok", true);
		        res.put("joinId", joinId);

		        // 화면 요약 필드만 내려줌
		        if (s.getAnnualContribAmt() != null) res.put("annualContribAmt", s.getAnnualContribAmt());
		        if (s.getNewContribAmt() != null)    res.put("newContribAmt", s.getNewContribAmt());
		        if (s.getBranchOffice() != null)     res.put("branchOffice", s.getBranchOffice());

		        return res;
			}
			case IRP_JOIN_INIT_OPEN: { // Step5 진입: 계약/계좌 번호만 사전 생성
				try {
			        Long joinId = requireLong(data, "joinId");

			        // 서비스가 계약/계좌번호를 생성하고 DTO로 반환
			        InitOpenResponse r = joinService.initOpen(joinId);

			        ObjectNode res = mapper.createObjectNode();
			        res.put("ok", true);
			        res.put("joinId", joinId);
			        res.put("irpAcctNo", r.getAcctNo());        // DTO의 계좌번호
			        res.put("contractNo", r.getContractNo());   // DTO의 계약번호
			        return res;

			    } catch (IllegalArgumentException | IllegalStateException e) {
			        ObjectNode err = mapper.createObjectNode();
			        err.put("ok", false);
			        err.put("error", e.getMessage() == null ? "BAD_REQUEST" : e.getMessage());
			        return err;
			    } catch (Exception e) {
			        ObjectNode err = mapper.createObjectNode();
			        err.put("ok", false);
			        err.put("error", "AP_INTERNAL_ERROR");
			        return err;
			    }
			}
			
			case IRP_JOIN_COMPLETE: { // Step5: (기존) 최종 완료: 비번 설정 + ACTIVE
				try {
			        Long joinId = requireLong(data, "joinId");
			        String irpPwd = data.path("irpPwd").asText(); // 4자리 숫자

			        AccountContractResult result =
			                joinService.completeJoinAndOpenIrpAccount(joinId, irpPwd);

			        ObjectNode res = mapper.createObjectNode();
			        res.put("ok", true);
			        res.put("joinId", joinId);
			        res.put("irpAcctNo", result.getIrpAcctNo());
			        res.put("contractNo", result.getContractNo());
			        return res;

			    } catch (IllegalArgumentException | IllegalStateException e) {
			        ObjectNode err = mapper.createObjectNode();
			        err.put("ok", false);
			        err.put("error", e.getMessage() == null ? "BAD_REQUEST" : e.getMessage());
			        return err;
			    } catch (Exception e) {
			        ObjectNode err = mapper.createObjectNode();
			        err.put("ok", false);
			        err.put("error", "AP_INTERNAL_ERROR");
			        return err;
			    }
            }
			/*
			case IRP_JOIN_UPDATE_PRODUCT: {		// step4: 상품 저장
                Long joinId = data.get("joinId").asLong();
                String productId = data.get("productId").asText();
                joinService.updateJoinProduct(joinId, productId);
                return mapper.createObjectNode().put("result", "OK");
            }*/
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
