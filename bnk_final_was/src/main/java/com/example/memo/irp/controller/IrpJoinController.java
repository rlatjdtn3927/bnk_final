package com.example.memo.irp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.irp.dto.IrpJoinDto;
import com.example.memo.irp.dto.RetirePurposeDto;
import com.example.memo.irp.dto.TaxPurposeDto;
import com.example.memo.irp.dto.TransferPurposeDto;
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
	
	@GetMapping("/join/{joinId}")
	@ResponseBody
	public Object getJoin(@PathVariable Long joinId) {
		JsonNode node = mapper.createObjectNode().put("joinId", joinId);
		TcpMessage msg = new TcpMessage(Command.IRP_JOIN_GET, node);
		return tcpService.sendMessage(msg);
	}
	
	/*IRP가입 - 계약정보등록 부분*/
	@PostMapping("/join")
    public Object createJoin(@RequestBody IrpJoinDto dto) {
        JsonNode node = mapper.convertValue(dto, JsonNode.class);
        TcpMessage msg = new TcpMessage(Command.IRP_JOIN_SAVE, node);
        return tcpService.sendMessage(msg); // join_id 반환
    }
	//가입목적-세액공제
	@PostMapping("/join/{joinId}/tax")
    public Object saveTax(@PathVariable Long joinId, @RequestBody TaxPurposeDto dto) {
        ObjectNode node = mapper.createObjectNode();
        node.put("joinId", joinId);
        node.put("irpQualType", dto.getIrpQualType());
        node.put("businessNo", dto.getBusinessNo());
        
        return tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_TAX_PURPOSE, node));
    }
	//가입목적-퇴직금 수령
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
	//가입목적-계약이전
	@PostMapping("/join/{joinId}/transfer")
	public Object saveTransfer(@PathVariable Long joinId,
	                                  @RequestBody TransferPurposeDto dto) {
	    ObjectNode node = mapper.createObjectNode();
	    node.put("joinId", joinId); // 경로에서 받은 값
	    node.put("prevBank", dto.getPrevBank());
	    node.put("prevAccount", dto.getPrevAccount());
	    node.put("transferAmt", dto.getTransferAmt());
	    node.put("transferDoc", dto.getTransferDoc());

	    return tcpService.sendMessage(new TcpMessage(Command.IRP_JOIN_TRANSFER_PURPOSE, node));
	}
	
	
}
