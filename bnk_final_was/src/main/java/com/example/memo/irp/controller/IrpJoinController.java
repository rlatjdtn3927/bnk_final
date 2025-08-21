package com.example.memo.irp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.irp.dto.BalanceReq;
import com.example.memo.irp.dto.ContractUpdateDto;
import com.example.memo.irp.dto.DraftJoinDto;
import com.example.memo.irp.dto.JoinCompleteDto;
import com.example.memo.irp.dto.RetirePurposeDto;
import com.example.memo.irp.dto.TaxPurposeDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/irp")
@RequiredArgsConstructor
@Slf4j
public class IrpJoinController {
	
	private final TcpClientService tcpService;
	private final ObjectMapper mapper;
	
	/*조회 - 사용자 정보(요약/검증용)*/
	@GetMapping("/join/{joinId}")
	@ResponseBody
	public ResponseEntity<?> getJoin(@PathVariable Long joinId) {
		JsonNode node = mapper.createObjectNode().put("joinId", joinId);
		return sendAndWrap(Command.IRP_JOIN_GET, node, "IRP_JOIN_GET");
	}
	//step1 - 가입목적 선택
	@PostMapping("/join/draft")
    public ResponseEntity<?> createDraft(@RequestBody DraftJoinDto dto, HttpSession session) {
		System.out.println("dto: " + dto);
		
		Long userId = (Long) session.getAttribute("userId"); // 로그인 시 저장된 userId
	    if (userId == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	    }
		
		ObjectNode node = mapper.createObjectNode()
                .put("userId", userId)
                .put("joinPurpose", dto.getJoinPurpose());
		
        return sendAndWrap(Command.IRP_JOIN_CREATE_DRAFT, node, "IRP_JOIN_CREATE_DRAFT");
	}
	
	//step1-2(세액공제)
	@PostMapping("/join/{joinId}/tax")
    public ResponseEntity<?> saveTax(@PathVariable("joinId") Long joinId, @RequestBody TaxPurposeDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("irpQualType", dto.getIrpQualType());
        node.put("businessNo", dto.getBusinessNo());
        
        return sendAndWrap(Command.IRP_JOIN_TAX_PURPOSE, node, "IRP_JOIN_TAX_PURPOSE");
    }
	//Step1-2(퇴직금 수령)
	@PostMapping("/join/{joinId}/retire")
    public ResponseEntity<?> saveRetire(@PathVariable("joinId") Long joinId, @RequestBody RetirePurposeDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("retireDate", dto.getRetireDate().toString());           // "2025-08-07"
        node.put("retireReason", dto.getRetireReason());       // 예: "정년퇴직"
        node.put("corpName", dto.getCorpName());               // 예: "삼성전자"
        node.put("severanceAmt", dto.getSeveranceAmt());       // 예: 15,000,000
        node.put("withholdDoc", dto.getWithholdDoc());         // 예: "퇴직확인서.pdf"
        return sendAndWrap(Command.IRP_JOIN_RETIRED_PURPOSE, node, "IRP_JOIN_RETIRED_PURPOSE");
    }
	
	/** 잔액조회: 사용자 소유 계좌일 때만 현재 잔액 반환 */
	@PostMapping("/join/{joinId}/account/balance")
	public ResponseEntity<?> getAccountBalance(@PathVariable("joinId") Long joinId,
			@RequestBody BalanceReq req) {
		ObjectNode node = mapper.createObjectNode()
				.put("joinId", joinId)
				.put("acctNo",  req.getAcctNo())
				.put("acctPwd", req.getAcctPwd()); // 옵션
		
		return sendAndWrap(Command.ACCOUNT_GET_BALANCE, node, "ACCOUNT_GET_BALANCE");
	}
	
	/** 출금계좌번호 조회 */
	@PostMapping("/join/{joinId}/account/no")
	public @ResponseBody ResponseEntity<?> getAccountNo(@PathVariable("joinId") Long joinId) {
		ObjectNode node = mapper.createObjectNode().put("joinId", joinId);
		return sendAndWrap(Command.ACCOUNT_GET_NUMBER, node, "ACCOUNT_GET_NUMBER");
	}
	
	//Step3: 계약정보 업데이트 (연한도/신규/출금계좌/관리영업점)
	@PutMapping("/join/{joinId}/contract")
    public ResponseEntity<?> updateContract(@PathVariable("joinId") Long joinId, 
    										@RequestBody ContractUpdateDto dto,
    										HttpSession session) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("annualContribAmt", dto.getAnnualContribAmt());
        node.put("newContribAmt", dto.getNewContribAmt());
        node.put("acctNo", dto.getAcctNo());
        node.put("acctPwd", dto.getAcctPwd());         // ★ 출금계좌 비번 전달
        node.put("branchOffice", dto.getBranchOffice());
        
        ResponseEntity<JsonNode> res =
                sendAndWrap(Command.IRP_JOIN_UPDATE_CONTRACT, node, "IRP_JOIN_UPDATE_CONTRACT");

        // ✅ 성공시에만 4단계 진입 허용 플래그 세팅
        if (res.getStatusCode().is2xxSuccessful()) {
            session.setAttribute(contractSavedKey(joinId), Boolean.TRUE);
        }
        return res;
    }
	
	//Step3: 조회
	@GetMapping("/join/{joinId}/contract")
	public ResponseEntity<?> getContract(@PathVariable("joinId") Long joinId) {
	    ObjectNode node = mapper.createObjectNode();
	    node.put("joinId", joinId);
	    return sendAndWrap(Command.IRP_JOIN_GET_CONTRACT, node, "IRP_JOIN_GET_CONTRACT");
	}
    
    //step4: 가입정보확인 (요약 페이지)
    @GetMapping("/join/{joinId}/infoConfir")
    public ResponseEntity<?> getInfoConfir(@PathVariable("joinId") Long joinId, HttpSession session) {
    	Object flag = session.getAttribute(contractSavedKey(joinId));
        boolean allowed = (flag instanceof Boolean) && ((Boolean) flag);
        if (!allowed) {
            ObjectNode body = mapper.createObjectNode()
                .put("ok", false)                                  // ✅ 일관 스키마
                .put("code", "PRECONDITION_REQUIRED")
                .put("message", "계약 정보 저장 후에 요약을 조회할 수 있습니다.");
            return ResponseEntity.status(428).body(body);          // 428
        }

        ObjectNode node = mapper.createObjectNode().put("joinId", joinId);
        ResponseEntity<JsonNode> res =
            sendAndWrap(Command.IRP_JOIN_PREPARE_OPEN, node, "IRP_JOIN_PREPARE_OPEN");

        // (선택) 캐시 방지 헤더
        return ResponseEntity.status(res.getStatusCode())
                .headers(h -> {
                    h.setCacheControl("no-store, no-cache, must-revalidate");
                    h.add("Pragma", "no-cache");
                })
                .body(res.getBody());
    }
    
    // step5 진입 시: 번호만 먼저 생성/조회
    @PostMapping("/join/{joinId}/init-open")
    public ResponseEntity<JsonNode> initOpen(@PathVariable("joinId") Long joinId) {
        ObjectNode node = mapper.createObjectNode().put("joinId", joinId);
        return sendAndWrap(Command.IRP_JOIN_INIT_OPEN, node, "IRP_JOIN_INIT_OPEN");
    }
	
	//step5(완료): (기존) 최종 완료: 비번 설정 + ACTIVE
    @PostMapping("/join/{joinId}/complete")
    public ResponseEntity<?> completeJoin(@PathVariable("joinId") Long joinId, 
    									  @RequestBody JoinCompleteDto dto, 
    									  HttpSession session) {
    	ObjectNode node = mapper.createObjectNode()
                .put("joinId", joinId)
                .put("irpPwd", dto.getIrpPwd()); // 4자리 숫자

        ResponseEntity<JsonNode> res = 
        		sendAndWrap(Command.IRP_JOIN_COMPLETE, node, "IRP_JOIN_COMPLETE");

        if (res.getStatusCode().is2xxSuccessful()) {
        	session.removeAttribute(contractSavedKey(joinId)); // 플래그 정리
        }
        return res;
    }
    
    /** 세션 키 상수 */
    private String contractSavedKey(long joinId) {
        return "contractSaved:" + joinId;
    }
    
    private void mask(ObjectNode n, String key) {
        if (n.has(key)) n.put(key, "***");
    }
    
    /** 로그에 민감정보 노출 방지용(단순 마스킹) */
    private JsonNode safePayloadForLog(JsonNode node) {
        try {
            ObjectNode copy = node.deepCopy();
            // 비번/문서 등 민감 키 마스킹
            mask(copy, "acctPwd");
            mask(copy, "irpPwd");
            return copy;
        } catch (Exception ignore) {
            return mapper.createObjectNode();
        }
    }
    
    /** 공통 TCP 호출 & 응답 래핑 */
    private ResponseEntity<JsonNode> sendAndWrap(Command cmd, JsonNode payload, String logCtx) {
    	try {
            JsonNode res = tcpService.sendMessage(new TcpMessage(cmd, payload));
            if (res == null || res.isNull()) {
                ObjectNode err = mapper.createObjectNode()
                    .put("ok", false)
                    .put("error", "UPSTREAM_EMPTY")
                    .put("message", "상위(AP) 응답이 비어 있습니다.");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
            }
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.warn("{} failed: payload={}, err={}", logCtx, safePayloadForLog(payload), e.toString());
            ObjectNode err = mapper.createObjectNode()
                .put("ok", false)
                .put("error", "INTERNAL_ERROR")
                .put("message", "서버 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
        }
    }
    /* step4: 상품선택(업데이트) 
    @PutMapping("/join/{joinId}/product")
    public ResponseEntity<?> updateProduct(@PathVariable("joinId") Long joinId, @RequestBody ProductMasterDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("productId", dto.getProductId());
        return sendAndWrap(Command.IRP_JOIN_UPDATE_PRODUCT, node, "IRP_JOIN_UPDATE_PRODUCT");
    }
     */
}
