package com.example.memo.company.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.memo.company.dto.CompanyLoginResponseDto;
import com.example.memo.company.dto.ContributionBatchValidateRequest;
import com.example.memo.company.dto.ContributionItemDto;
import com.example.memo.company.utils.ExcelParser;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/company/dc/contribution")
@RequiredArgsConstructor
public class DcContributionController {

    private final TcpClientService tcpService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadContributionFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("paymentDate") String paymentDate,
            HttpSession session) {

        CompanyLoginResponseDto login = (CompanyLoginResponseDto) session.getAttribute("loginManager");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","로그인이 필요합니다."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message","파일이 비어 있습니다."));
        }

        // 날짜 형식 체크
        try { LocalDate.parse(paymentDate); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message","납입예정일 형식 오류(yyyy-MM-dd)")); }

        try {
            List<ContributionItemDto> items = ExcelParser.parseContribution(file);

            long totalAmount = items.stream()
                    .mapToLong(i -> parseLong(i.getAmount()) + parseLong(i.getPerformanceBonus()))
                    .sum();

            ObjectNode data = objectMapper.createObjectNode();
            data.put("companyId", login.getCompanyId());
            data.put("uploaderId", login.getManagerId());
            data.put("fileName", file.getOriginalFilename());
            data.put("totalRecords", items.size());
            data.put("totalAmount", totalAmount);
            data.put("paymentDate", paymentDate);

            List<Map<String, String>> itemMaps = items.stream().map(i -> {
                Map<String,String> m = new HashMap<>();
                m.put("name", nv(i.getName()));
                m.put("ssn", nv(i.getSsn()));
                m.put("amount", nv(i.getAmount()));
                m.put("performanceBonus", nv(i.getPerformanceBonus()));
                m.put("annualSalary", nv(i.getAnnualSalary()));
                return m;
            }).collect(Collectors.toList());
            data.putPOJO("items", itemMaps);

            TcpMessage msg = new TcpMessage(Command.CONTRIBUTION_BATCH_CREATE, data);
            Object resp = tcpService.sendMessage(msg);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "파일 처리 중 오류: " + e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateContributionBatch(@RequestBody ContributionBatchValidateRequest payload) {
        if (payload == null || payload.getBatchId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message","batchId가 필요합니다."));
        }
        ObjectNode data = objectMapper.createObjectNode().put("batchId", payload.getBatchId());
        TcpMessage msg = new TcpMessage(Command.CONTRIBUTION_BATCH_VALIDATE, data);
        return ResponseEntity.ok(tcpService.sendMessage(msg));
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestParam("batchId") Long batchId, HttpSession session) {
        CompanyLoginResponseDto login = (CompanyLoginResponseDto) session.getAttribute("loginManager");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","로그인이 필요합니다."));
        }
        ObjectNode data = objectMapper.createObjectNode()
            .put("batchId", batchId)
            .put("companyId", login.getCompanyId()); // 소유 확인용
        TcpMessage msg = new TcpMessage(Command.CONTRIBUTION_BATCH_CONFIRM, data);
        return ResponseEntity.ok(tcpService.sendMessage(msg));
    }
    
    @GetMapping("/plan-list")
    public ResponseEntity<?> planList(
    	    @RequestParam(name = "fromDate", required = false) String fromDate,
    	    @RequestParam(name = "toDate",   required = false) String toDate,
    	    @RequestParam(name = "status",   required = false) String status,
    	    @RequestParam(name = "keyword",  required = false) String keyword,
    	    @RequestParam(name = "page",     defaultValue = "0") int page,
    	    @RequestParam(name = "size",     defaultValue = "20") int size,
            HttpSession session) {

        CompanyLoginResponseDto login = (CompanyLoginResponseDto) session.getAttribute("loginManager");
        if (login == null) return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("companyId", login.getCompanyId());
        if (fromDate != null && !fromDate.isBlank()) data.put("fromDate", fromDate);
        if (toDate   != null && !toDate.isBlank())   data.put("toDate", toDate);
        if (status   != null && !status.isBlank())   data.put("status", status);
        if (keyword  != null && !keyword.isBlank())  data.put("keyword", keyword);
        data.put("page", page);
        data.put("size", size);

        TcpMessage msg = new TcpMessage(Command.CONTRIBUTION_BATCH_LIST, data);
        return ResponseEntity.ok(tcpService.sendMessage(msg));
    }
    
    @GetMapping("/source-accounts")
    public ResponseEntity<?> sourceAccounts(HttpSession session) {
        CompanyLoginResponseDto login = (CompanyLoginResponseDto) session.getAttribute("loginManager");
        if (login == null) return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));

        ObjectNode data = objectMapper.createObjectNode().put("companyId", login.getCompanyId());
        TcpMessage msg = new TcpMessage(Command.CONTRIBUTION_COMPANY_ACCOUNT_LIST, data);
        return ResponseEntity.ok(tcpService.sendMessage(msg));
    }
    
    @PostMapping("/execute")
    public ResponseEntity<?> executeContribution(
            @RequestParam("batchId") Long batchId,
            @RequestParam("sourceAccountId") Long sourceAccountId,
            HttpSession session) {

        var login = (CompanyLoginResponseDto) session.getAttribute("loginManager");
        if (login == null) return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));

        ObjectNode data = objectMapper.createObjectNode()
                .put("batchId", batchId)
                .put("sourceAccountId", sourceAccountId)
                .put("companyId", login.getCompanyId());

        return ResponseEntity.ok(
            tcpService.sendMessage(new TcpMessage(Command.CONTRIBUTION_BATCH_EXECUTE, data))
        );
    }


    private static long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        return Long.parseLong(s.replaceAll("[,\\s]", ""));
    }
    private static String nv(String s) { return s == null ? "" : s.trim(); }
}