package com.example.memo.irp.controller;

import java.util.Map;

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

@RestController
@RequestMapping("/irp")
@RequiredArgsConstructor
public class IrpJoinController {
	
	private final TcpClientService tcpService;
	private final ObjectMapper mapper;
	
	/*조회 - 사용자 정보(요약/검증용)*/
	@GetMapping("/join/{joinId}")
	@ResponseBody
	public Object getJoin(@PathVariable Long joinId) {
		JsonNode node = mapper.createObjectNode().put("joinId", joinId);
		TcpMessage msg = new TcpMessage(Command.IRP_JOIN_GET, node);
		return tcpService.sendMessage(msg);
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
	@PostMapping("/join/draft")
    public ResponseEntity<?> createDraft(@RequestBody DraftJoinDto dto) {
		System.out.println("dto: " + dto);
        ObjectNode node = mapper.createObjectNode();
        node.put("userId", dto.getUserId());
        node.put("joinPurpose", dto.getJoinPurpose());
        
        JsonNode res;
        
        try {
            res = tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_CREATE_DRAFT, node));
        } catch (Exception e) {
            // TCP 레벨에서 예외 발생 시
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "TCP_ERROR", "detail", e.getMessage()));
        }
        
        if (res == null) {
            // 여기로 온다면 지금의 200 No Content 원인 확정
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error","AP 응답 없음","detail","sendMessage returned null"));
        }
        
        return ResponseEntity.ok(res);
	}
	
	//step3-1(세액공제)
	@PostMapping("/join/{joinId}/tax")
    public Object saveTax(@PathVariable("joinId") Long joinId, @RequestBody TaxPurposeDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("irpQualType", dto.getIrpQualType());
        node.put("businessNo", dto.getBusinessNo());
        
        return tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_TAX_PURPOSE, node));
    }
	//Step3-1(퇴직금 수령)
	@PostMapping("/join/{joinId}/retire")
    public Object saveRetire(@PathVariable Long joinId, @RequestBody RetirePurposeDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("retireDate", dto.getRetireDate().toString());           // "2025-08-07"
        node.put("retireReason", dto.getRetireReason());       // 예: "정년퇴직"
        node.put("corpName", dto.getCorpName());               // 예: "삼성전자"
        node.put("severanceAmt", dto.getSeveranceAmt());       // 예: 15,000,000
        node.put("withholdDoc", dto.getWithholdDoc());         // 예: "퇴직확인서.pdf"
        return tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_RETIRED_PURPOSE, node));
    }
	
	//Step3-2: 계약정보 업데이트 (연한도/신규/출금계좌/관리영업점)
	@PutMapping("/join/{joinId}/contract")
    public Object updateContract(@PathVariable Long joinId, @RequestBody ContractUpdateDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("annualContribAmt", dto.getAnnualContribAmt());
        node.put("newContribAmt", dto.getNewContribAmt());
        node.put("acctNo", dto.getAcctNo());
        node.put("acctPwd", dto.getAcctPwd());         // ★ 출금계좌 비번 전달
        node.put("branchOffice", dto.getBranchOffice());
        return tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_UPDATE_CONTRACT, node));
    }
	
	/* step4: 상품선택(업데이트) */
    @PutMapping("/join/{joinId}/product")
    public Object updateProduct(@PathVariable Long joinId, @RequestBody ProductMasterDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("productId", dto.getProductId());
        return tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_UPDATE_PRODUCT, node));
    }
	
	/* 가입완료 → IRP계좌 개설 (계좌번호와 계약번호는 서버에서 자동 생성)*/
    @PostMapping("/join/{joinId}/complete")
    public Object completeJoin(@PathVariable Long joinId, @RequestBody JoinCompleteDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("irpPwd", dto.getIrpPwd());	// 신규 IRP계좌 비밀번호
        return tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_COMPLETE, node));
    }
}
