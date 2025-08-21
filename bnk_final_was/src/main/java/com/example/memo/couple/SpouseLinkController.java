package com.example.memo.couple;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/link")
public class SpouseLinkController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    /**
     * 부부연동 신청 (WAS → AP)
     * - form-data: spouseName, spouseBirth(yyyy-MM-dd), file(optional), existingLinkId(optional)
     * - 세션 키: "user" (Long/Integer/String OK)
     */
    @PostMapping(value = "/apply",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> apply(@RequestParam("spouseName") String spouseName,
                                   @RequestParam("spouseBirth") String spouseBirth,
                                   @RequestPart(name = "file", required = false) MultipartFile file,
                                   // ▼▼▼ 재시도를 위한 파라미터 추가 ▼▼▼
                                   @RequestParam(name = "existingLinkId", required = false) Long existingLinkId,
                                   HttpSession session) {
        Map<String, Object> body = new HashMap<>();
        try {
            // 0) 파라미터 검증
            if (!StringUtils.hasText(spouseName) || !StringUtils.hasText(spouseBirth)) {
                body.put("success", false);
                body.put("code", "BAD_REQUEST");
                body.put("message", "배우자 이름/생년월일을 입력하세요.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            // 1) 세션에서 사용자 ID 얻기 ("user" 하나만 사용)
            Object v = session.getAttribute("user");
            if (v == null) {
                body.put("success", false);
                body.put("code", "UNAUTHORIZED");
                body.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            Long applicantUserId;
            try {
                applicantUserId = (v instanceof Long) ? (Long) v
                        : (v instanceof Integer) ? ((Integer) v).longValue()
                        : Long.valueOf(v.toString());
            } catch (Exception e) {
                body.put("success", false);
                body.put("code", "SESSION_TYPE_ERROR");
                body.put("message", "세션 사용자 정보 형식 오류");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            // 2) AP로 보낼 payload 조립
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("applicantUserId", applicantUserId);
            payload.put("spouseName", spouseName);
            payload.put("spouseBirth", spouseBirth);

            // ▼▼▼ existingLinkId가 있으면 payload에 추가 ▼▼▼
            if (existingLinkId != null) {
                payload.put("existingLinkId", existingLinkId);
            }

            if (file != null && !file.isEmpty()) {
                ObjectNode f = objectMapper.createObjectNode();
                f.put("filename", file.getOriginalFilename());
                f.put("contentType", file.getContentType());
                f.put("base64", Base64.getEncoder().encodeToString(file.getBytes()));
                payload.set("file", f);
            }

            // 3) TCP 전송 (암/복호화는 TcpClientService 내부)
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_APPLY, payload);
            JsonNode apRes = tcpClientService.sendMessage(msg);

            // 4) 응답 처리 (AP 응답이 null이거나 에러인 경우)
            if (apRes == null) {
                body.put("success", false);
                body.put("code", "AP_UNREACHABLE");
                body.put("message", "AP 응답 없음");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
            }

            // AP 응답이 에러 형식일 경우 그대로 클라이언트에 전달
            if (apRes.has("success") && !apRes.get("success").asBoolean()) {
                return ResponseEntity.badRequest().body(apRes);
            }

            // 5) 성공 응답 그대로 래핑하여 반환
            body.put("success", true);
            body.put("code", "OK");
            body.put("data", apRes); // { linkId, status, nextAction, ... }
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            body.put("success", false);
            body.put("code", "INTERNAL_ERROR");
            body.put("message", "신청 처리 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
    
    
    /**
     * 배우자가 연동 요청을 수락/거절하는 API
     */
    @PostMapping("/spouse/decide")
    public ResponseEntity<?> decideBySpouse(@RequestBody JsonNode payload, HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) {
            return ResponseEntity.status(401).build(); // 비로그인 사용자는 401 Unauthorized
        }

        // AP로 보낼 payload에 세션의 사용자 ID(배우자 ID)를 추가
        ((ObjectNode) payload).put("spouseUserId", userId);

        TcpMessage message = new TcpMessage(Command.SPOUSE_LINK_DECIDE, payload);
        JsonNode apResponse = tcpClientService.sendMessage(message);

        return ResponseEntity.ok(apResponse);
    }
    
    /**
     * 사용자가 OCR 3회 실패 후 관리자 검토를 직접 요청하는 API
     * 
     */
    @PostMapping(
    	    value = "/request-admin-review",
    	    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    	    produces = MediaType.APPLICATION_JSON_VALUE
    	)
    	public ResponseEntity<?> requestAdminReview(
    	        @RequestParam("linkId") Long linkId,
    	        @RequestParam("spouseName") String spouseName,
    	        @RequestParam("spouseBirth") String spouseBirth,
    	        @RequestPart("file") MultipartFile file, // 반드시 첨부
    	        HttpSession session) {

    	    Long userId = (Long) session.getAttribute("user");
    	    if (userId == null) {
    	        return ResponseEntity.status(401).build();
    	    }
    	    try {
    	        var payload = objectMapper.createObjectNode();
    	        payload.put("linkId", linkId);
    	        payload.put("spouseName", spouseName);
    	        payload.put("spouseBirth", spouseBirth);

    	        var f = objectMapper.createObjectNode();
    	        f.put("filename", file.getOriginalFilename());
    	        f.put("contentType", file.getContentType());
    	        f.put("base64", Base64.getEncoder().encodeToString(file.getBytes()));
    	        payload.set("file", f);

    	        TcpMessage message =
    	            new TcpMessage(Command.SPOUSE_LINK_REQUEST_ADMIN_REVIEW, payload);
    	        JsonNode apResponse = tcpClientService.sendMessage(message);
    	        return ResponseEntity.ok(apResponse);
    	    } catch (Exception e) {
    	        var err = objectMapper.createObjectNode();
    	        err.put("success", false).put("code", "INTERNAL_ERROR")
    	           .put("message", e.getMessage());
    	        return ResponseEntity.status(500).body(err);
    	    }
    	}
    
    @GetMapping("/status")
    public ResponseEntity<?> getLinkStatus(HttpSession session) {
        Map<String, Object> body = new HashMap<>();
        try {
            Object v = session.getAttribute("user");
            if (v == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }
            Long userId = (v instanceof Long) ? (Long) v
                    : (v instanceof Integer) ? ((Integer) v).longValue()
                    : Long.valueOf(v.toString());

            ObjectNode payload = objectMapper.createObjectNode().put("userId", userId);
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_STATUS, payload);
            JsonNode apRes = tcpClientService.sendMessage(msg);

            // AP 표준 응답 그대로 전달
            return ResponseEntity.ok(apRes);

        } catch (Exception e) {
            body.put("success", false);
            body.put("code", "INTERNAL_ERROR");
            body.put("message", "상태 조회 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /**
     * 연동 신청 상세 정보 조회 (신청자 이름 등)
     */
    @GetMapping("/details/{linkId}")
    public ResponseEntity<?> getLinkDetails(@PathVariable("linkId") Long linkId) {
        try {
            // 1. AP로 보낼 payload 생성
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("linkId", linkId);

            // 2. TCP 메시지 생성 및 전송
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_GET_DETAILS, payload);
            JsonNode apResponse = tcpClientService.sendMessage(msg);

            // 3. AP 응답 그대로 반환
            if (apResponse == null || (apResponse.has("success") && !apResponse.get("success").asBoolean())) {
                return ResponseEntity.badRequest().body(apResponse);
            }
            return ResponseEntity.ok(apResponse);

        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", "상세 정보 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }
    
}