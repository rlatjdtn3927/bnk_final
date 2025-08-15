package com.example.memo.admin.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.admin.dto.BankEmployeeLoginDto;
import com.example.memo.admin.dto.BulkApprovalRequestDto;
import com.example.memo.admin.dto.BulkRejectionRequestDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/admin/requests") // 관리자용 URL 경로
public class AdminRequestController {

    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    public AdminRequestController(TcpClientService tcpService, ObjectMapper objectMapper) {
        this.tcpService = tcpService;
        this.objectMapper = objectMapper;
    }

    /**
     * 승인 대기중인 계좌 개설 요청 목록을 조회하는 API
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        try {
        	
            JsonNode emptyData = objectMapper.createObjectNode();

            TcpMessage requestMsg = new TcpMessage(Command.ACCOUNT_REQUEST_GET_PENDING, emptyData);

            Object response = tcpService.sendMessage(requestMsg);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("요청 목록 조회 중 오류가 발생했습니다.");
        }
    }
    
    @PostMapping("/approve-bulk")
    public ResponseEntity<?> bulkApprove(@RequestBody BulkApprovalRequestDto dto, HttpSession session) {
        try {
            Long approverId = getApproverIdFromSession(session);
            if (approverId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
            }

            ObjectNode data = objectMapper.createObjectNode();
            data.putPOJO("requestIds", dto.getRequestIds());
            data.put("approverId", approverId);
            
            TcpMessage msg = new TcpMessage(Command.ACCOUNT_REQUEST_APPROVE_BULK, data);
            return ResponseEntity.ok(tcpService.sendMessage(msg));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "승인 처리 중 서버 오류가 발생했습니다."));
        }
    }
    
    
    @PostMapping("/reject-bulk")
    public ResponseEntity<?> bulkReject(@RequestBody BulkRejectionRequestDto dto, HttpSession session) {
        try {
            Long approverId = getApproverIdFromSession(session);
            if (approverId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
            }

            ObjectNode data = objectMapper.createObjectNode();
            data.putPOJO("requestIds", dto.getRequestIds());
            data.put("reason", dto.getReason());
            data.put("approverId", approverId);
            
            TcpMessage msg = new TcpMessage(Command.ACCOUNT_REQUEST_REJECT_BULK, data);
            return ResponseEntity.ok(tcpService.sendMessage(msg));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "거절 처리 중 서버 오류가 발생했습니다."));
        }
    }
    
    // ★ 추가: 승인 목록
    @GetMapping("/approved")
    public ResponseEntity<?> getApprovedRequests() {
        try {
            JsonNode empty = objectMapper.createObjectNode();
            TcpMessage msg = new TcpMessage(Command.ACCOUNT_REQUEST_GET_APPROVED, empty);
            Object res = tcpService.sendMessage(msg);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("승인 목록 조회 중 오류가 발생했습니다.");
        }
    }

    // ★ 추가: 거절 목록
    @GetMapping("/rejected")
    public ResponseEntity<?> getRejectedRequests() {
        try {
            JsonNode empty = objectMapper.createObjectNode();
            TcpMessage msg = new TcpMessage(Command.ACCOUNT_REQUEST_GET_REJECTED, empty);
            Object res = tcpService.sendMessage(msg);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("거절 목록 조회 중 오류가 발생했습니다.");
        }
    }
    
    private Long getApproverIdFromSession(HttpSession session) {
        BankEmployeeLoginDto loginEmployee = (BankEmployeeLoginDto) session.getAttribute("loginEmployee");
        return (loginEmployee != null) ? loginEmployee.getId() : null;
    }
    
    /*아래로 상품 크롤링 데이터 관련 매핑 함수들*/
    @GetMapping("/crawl") 
    public ResponseEntity<?> getUpdateList() {
    	JsonNode emptyData = objectMapper.createObjectNode();
        TcpMessage requestMsg = new TcpMessage(Command.ADMIN_CRAWL_CHECK_UPDATE, emptyData);
        Object response = tcpService.sendMessage(requestMsg);
        
    	return ResponseEntity.status(HttpStatus.OK).body("");
    }
    
}
