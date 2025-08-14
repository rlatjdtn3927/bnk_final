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

import com.example.memo.irp.dto.ContractUpdateDto;
import com.example.memo.irp.dto.DraftJoinDto;
import com.example.memo.irp.dto.JoinCompleteDto;
import com.example.memo.irp.dto.ProductMasterDto;
import com.example.memo.irp.dto.RetirePurposeDto;
import com.example.memo.irp.dto.TaxPurposeDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	/* 수정 -> 생성방식 분리
	//IRP가입 - 계약정보등록 부분
	@PostMapping("/join")
    public Object createJoin(@RequestBody IrpJoinDto dto) {
        JsonNode node = mapper.convertValue(dto, JsonNode.class);
        TcpMessage msg = new TcpMessage(Command.IRP_JOIN_SAVE, node);
        return tcpService.sendMessage(msg); // join_id 반환
    }
	*/
	//step1 - 가입목적 선택
	@PostMapping("/join/draft")
    public ResponseEntity<?> createDraft(@RequestBody DraftJoinDto dto) {
		System.out.println("dto: " + dto);
		ObjectNode node = mapper.createObjectNode()
                .put("userId", dto.getUserId())
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
	
	//Step3: 계약정보 업데이트 (연한도/신규/출금계좌/관리영업점)
	@PutMapping("/join/{joinId}/contract")
    public ResponseEntity<?> updateContract(@PathVariable("joinId") Long joinId, @RequestBody ContractUpdateDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("annualContribAmt", dto.getAnnualContribAmt());
        node.put("newContribAmt", dto.getNewContribAmt());
        node.put("acctNo", dto.getAcctNo());
        node.put("acctPwd", dto.getAcctPwd());         // ★ 출금계좌 비번 전달
        node.put("branchOffice", dto.getBranchOffice());
        return sendAndWrap(Command.IRP_JOIN_UPDATE_CONTRACT, node, "IRP_JOIN_UPDATE_CONTRACT");
    }
	
	/* step4: 상품선택(업데이트) */
    @PutMapping("/join/{joinId}/product")
    public ResponseEntity<?> updateProduct(@PathVariable("joinId") Long joinId, @RequestBody ProductMasterDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("productId", dto.getProductId());
        return sendAndWrap(Command.IRP_JOIN_UPDATE_PRODUCT, node, "IRP_JOIN_UPDATE_PRODUCT");
    }
	
	/* 가입완료 → IRP계좌 개설 (계좌번호와 계약번호는 서버에서 자동 생성)*/
    @PostMapping("/join/{joinId}/complete")
    public ResponseEntity<?> completeJoin(@PathVariable("joinId") Long joinId, @RequestBody JoinCompleteDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("irpPwd", dto.getIrpPwd());	// 신규 IRP계좌 비밀번호
        return sendAndWrap(Command.IRP_JOIN_COMPLETE, node, "IRP_JOIN_COMPLETE");
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
                // 비즈니스적으로 빈 응답을 허용해야 한다면 204로도 고려 가능
                return ResponseEntity.ok(mapper.createObjectNode());
            }
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.warn("{} failed: payload={}, err={}", logCtx, safePayloadForLog(payload), e.toString());
            ObjectNode err = mapper.createObjectNode()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", "서버 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
