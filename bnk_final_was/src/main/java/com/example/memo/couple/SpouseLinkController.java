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
     * - form-data: spouseName, spouseBirth(yyyy-MM-dd), file(optional)
     * - 세션 키: "user" (Long/Integer/String OK)
     */
    @PostMapping(value = "/apply",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> apply(@RequestParam("spouseName") String spouseName,
                                   @RequestParam("spouseBirth") String spouseBirth,
                                   @RequestPart(name = "file", required = false) MultipartFile file,
                                   HttpServletRequest request,
                                   HttpSession session) {
        Map<String, Object> body = new HashMap<>();
        try {
            // 0) 파라미터 검증
            if (!StringUtils.hasText(spouseName) || !StringUtils.hasText(spouseBirth)) {
                body.put("success", false);
                body.put("code", "BAD_REQUEST");
                body.put("message", "배우자 이름/생년월일을 입력하세요.");
                body.put("data", null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            // 1) 세션에서 사용자 ID 얻기 ("user" 하나만 사용)
            Object v = session.getAttribute("user");
            if (v == null) {
                body.put("success", false);
                body.put("code", "UNAUTHORIZED");
                body.put("message", "로그인이 필요합니다.");
                body.put("data", null);
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
                body.put("data", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            // 2) AP로 보낼 payload 조립
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("applicantUserId", applicantUserId);
            payload.put("spouseName", spouseName);
            payload.put("spouseBirth", spouseBirth);

            if (file != null && !file.isEmpty()) {
                ObjectNode f = objectMapper.createObjectNode();
                f.put("filename", file.getOriginalFilename());
                f.put("contentType", file.getContentType());
                // 한 줄 프로토콜을 위해 개행 없는 Base64
                f.put("base64", Base64.getEncoder().encodeToString(file.getBytes()));
                payload.set("file", f);
            }

            // 3) TCP 전송 (암/복호화는 TcpClientService 내부)
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_APPLY, payload);
            JsonNode apRes = tcpClientService.sendMessage(msg);

            if (apRes == null) {
                body.put("success", false);
                body.put("code", "AP_UNREACHABLE");
                body.put("message", "AP 응답 없음");
                body.put("data", null);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
            }

            // AP 표준 에러 처리: {"error": true, "message": "..."}
            if (apRes.has("error") && apRes.get("error").asBoolean()) {
                body.put("success", false);
                body.put("code", "AP_ERROR");
                body.put("message", apRes.has("message") ? apRes.get("message").asText() : "처리 실패");
                body.put("data", null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }
            
            if (file != null && !file.isEmpty()) {
                String ct = file.getContentType();
                String nm = file.getOriginalFilename();
                boolean isPdf = (ct != null && ct.toLowerCase().contains("pdf"))
                             || (nm != null && nm.toLowerCase().endsWith(".pdf"));
                if (!isPdf) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false, "code", "ONLY_PDF",
                        "message", "PDF 파일만 업로드할 수 있습니다.", "data", null));
                }
            }

            // 4) 성공 응답 그대로 래핑하여 반환
            body.put("success", true);
            body.put("code", "OK");
            body.put("message", "신청 완료");
            body.put("data", apRes); // { linkId, status, nextAction, ... }
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            body.put("success", false);
            body.put("code", "INTERNAL_ERROR");
            body.put("message", "신청 처리 중 오류: " + e.getMessage());
            body.put("data", null);
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
}