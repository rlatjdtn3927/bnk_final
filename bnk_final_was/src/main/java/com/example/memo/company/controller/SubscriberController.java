package com.example.memo.company.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.memo.company.dto.CompanyLoginResponseDto;
import com.example.memo.company.dto.SubscriberDto;
import com.example.memo.company.utils.ExcelParser;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/company/subscriber")
public class SubscriberController {
	
    @Autowired
    private TcpClientService tcpService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(SubscriberController.class);

    /**
     * 엑셀 업로드 및 데이터 검증을 한 번에 처리
     */
    @PostMapping("/excel-upload")
    public ResponseEntity<?> uploadAndValidateExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        try {
            // 1. 엑셀 파싱
            List<SubscriberDto> dtoList = ExcelParser.parse(file);

            // 2. 세션에서 회사 ID 주입
            try {
                // 1. 세션에서 로그인 정보를 가져옵니다.
                Object attribute = session.getAttribute("loginManager");
                if (attribute == null) {
                    return ResponseEntity.status(401).body("세션이 만료되었습니다. 다시 로그인해주세요.");
                }

                // 2. 로그인 정보에서 companyId를 추출합니다.
                CompanyLoginResponseDto loginManager = (CompanyLoginResponseDto) attribute;
                Long companyId = loginManager.getCompanyId();
                if (companyId == null) {
                     return ResponseEntity.status(401).body("세션에 회사 정보가 없습니다.");
                }

                // 3. 클라이언트가 보낸 모든 데이터에 서버의 companyId를 강제로 다시 설정합니다.
                dtoList.forEach(dto -> dto.setCompanyId(companyId));

            } catch (ClassCastException e) {
                return ResponseEntity.internalServerError().body("세션 데이터 형식이 올바르지 않습니다.");
            }
            
            // 3. AP 서버에 검증 요청
            JsonNode data = objectMapper.convertValue(dtoList, JsonNode.class);
            TcpMessage msg = new TcpMessage(Command.SUBSCRIBER_VALIDATE, data); // ✅ VALIDATE 커맨드 사용
            Object response = tcpService.sendMessage(msg);

            return ResponseEntity.ok(response); // AP의 검증 결과를 그대로 클라이언트에 전달

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("엑셀 파일 처리 또는 검증 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 검증 완료된 데이터 최종 등록
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerSubscribers(@RequestBody List<SubscriberDto> dtoList, HttpSession session) {
        Command cmd = Command.SUBSCRIBER_REGISTER;

        // Controller에서 companyId를 다시 넣을 필요 없이 클라이언트가 보내준 것을 그대로 사용
        // 단, 보안을 위해 dtoList의 companyId가 세션의 companyId와 일치하는지 확인하는 로직 추가 가능
        
        JsonNode data = objectMapper.convertValue(dtoList, JsonNode.class);
        TcpMessage msg = new TcpMessage(cmd, data);
        Object response = tcpService.sendMessage(msg);

        return ResponseEntity.ok(response);
    }
    
    
    @GetMapping("/list")
    public ResponseEntity<?> getSubscriberList(
            @RequestParam(name = "status", defaultValue = "ALL") String status, // 1. status 파라미터 추가
            HttpSession session) {
        
        try {
            // 세션에서 companyId 추출
            CompanyLoginResponseDto loginManager = (CompanyLoginResponseDto) session.getAttribute("loginManager");
            if (loginManager == null) {
                return ResponseEntity.status(401).body("세션이 만료되었습니다.");
            }
            Long companyId = loginManager.getCompanyId();

            // 2. AP에 보낼 TCP 메시지에 status도 추가
            ObjectNode data = objectMapper.createObjectNode();
            data.put("companyId", companyId);
            data.put("status", status); // status 값을 데이터에 포함
            TcpMessage msg = new TcpMessage(Command.SUBSCRIBER_GET_LIST, data);

            // AP에 메시지 전송 및 응답 반환
            Object response = tcpService.sendMessage(msg);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("가입자 명부 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("가입자 명부 조회 중 오류가 발생했습니다.");
        }
    }
}