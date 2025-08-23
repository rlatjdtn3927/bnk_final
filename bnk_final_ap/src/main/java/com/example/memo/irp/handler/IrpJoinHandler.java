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
				try {
			        Long joinId    = requireLong(data, "joinId");
			        String qual    = requireText(data, "irpQualType");
			        String busiNo  = optText(data, "businessNo"); // nullable

			        joinService.saveTaxPurpose(joinId, qual, busiNo);

			        return mapper.createObjectNode()
			                     .put("ok", true)
			                     .put("joinId", joinId);
			    } catch (Exception e) {
			        return mapper.createObjectNode()
			                     .put("ok", false)
			                     .put("error", e.getMessage() == null ? "AP_INTERNAL" : e.getMessage());
			    }
			}
			case IRP_JOIN_RETIRED_PURPOSE: {
				try {
			        Long joinId = requireLong(data, "joinId");
			        String retireDateStr = requireText(data, "retireDate"); // "YYYY-MM-DD"
			        LocalDate retireDate = LocalDate.parse(retireDateStr);

			        String reason   = optText(data, "retireReason");             // nullable 허용
			        String corpName = requireText(data, "corpName");             // NOT NULL 컬럼
			        Long   amt      = optLong(data, "severanceAmt");             // NOT NULL 컬럼
			        String doc      = optText(data, "withholdDoc");              // nullable 허용

			        // 필수값 검증 (DB 제약과 동일하게)
			        if (amt == null) throw new IllegalArgumentException("severanceAmt required");
			        if (corpName == null || corpName.isBlank()) throw new IllegalArgumentException("corpName required");

			        // 서비스는 '업서트' 
			        joinService.saveRetirePurpose(joinId, retireDate, reason, corpName, amt, doc);

			        return mapper.createObjectNode()
			                     .put("ok", true)
			                     .put("joinId", joinId);

			    } catch (Exception e) {
			        ObjectNode err = mapper.createObjectNode();
			        err.put("ok", false);
			        err.put("error", e.getMessage() == null ? "AP_INTERNAL" : e.getMessage());
			        return err; // 절대로 null/문자열만 반환하지 않음
			    }
			}
			case IRP_JOIN_CHECK_ELIGIBLE: {
			    long userId = data.path("userId").asLong();
			    boolean active = joinService.hasActiveIrp(userId); // ACTIVE만 체크

			    ObjectNode res = mapper.createObjectNode()
			        .put("ok", true)
			        .put("eligible", !active);

			    if (active) {
			        res.put("code", "ALREADY_JOINED")
			           .put("message", "이미 IRP에 가입하셨습니다.");
			    }
			    return res;
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
			        res.put("irpAcctNo", r.getIrpAcctNo());        // DTO의 계좌번호
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
	
	private String optText(JsonNode n, String field) {
	    JsonNode v = n.get(field);
	    return (v == null || v.isNull()) ? null : v.asText();
	}
	private Long optLong(JsonNode n, String field) {
	    JsonNode v = n.get(field);
	    return (v == null || v.isNull()) ? null : v.asLong();
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
