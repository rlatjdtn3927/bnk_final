package com.example.memo.couple;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@RequiredArgsConstructor
@RestController
@RequestMapping("/link")
public class SpouseLinkController {

    private final TcpClientService tcpClientService;
    private final ObjectMapper objectMapper;

    /**
     * 부부연동 신청 (WAS → AP)
     */
    @PostMapping(value = "/apply",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> apply(@RequestParam(name="linkId", required=false) Long linkId, // ✅ 추가
                                   @RequestParam("spouseName") String spouseName,
                                   @RequestParam("spouseBirth") String spouseBirth,
                                   @RequestPart(name = "file", required = false) MultipartFile file,
                                   HttpSession session) {
        try {
            Long applicantUserId = getUserIdFromSession(session);

            ObjectNode payload = objectMapper.createObjectNode();
            if (linkId != null) payload.put("linkId", linkId); // ✅ 재시도 시 필요
            payload.put("applicantUserId", applicantUserId);
            payload.put("spouseName", spouseName);
            payload.put("spouseBirth", spouseBirth);

            if (file != null && !file.isEmpty()) {
                ObjectNode f = objectMapper.createObjectNode();
                f.put("filename", file.getOriginalFilename());
                f.put("contentType", file.getContentType());
                f.put("base64", Base64.getEncoder().encodeToString(file.getBytes()));
                payload.set("file", f);
            }

            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_APPLY, payload);
            JsonNode apRes = tcpClientService.sendMessage(msg);
            return ResponseEntity.ok(Map.of("success", true, "code", "OK", "data", apRes));

        } catch (AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * 배우자가 연동 요청을 수락/거절하는 API
     */
    @PostMapping("/spouse/decide")
    public ResponseEntity<?> decideBySpouse(@RequestBody JsonNode payload, HttpSession session) {
        try {
            Long spouseUserId = getUserIdFromSession(session);
            ((ObjectNode) payload).put("spouseUserId", spouseUserId);

            TcpMessage message = new TcpMessage(Command.SPOUSE_LINK_DECIDE, payload);
            JsonNode apResponse = tcpClientService.sendMessage(message);

            return ResponseEntity.ok(apResponse);
        } catch (AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    // =================================================================
    // ▼▼▼ [추가] 연동 해지 API ▼▼▼
    // =================================================================
    /**
     * 부부 IRP 연동을 해지합니다.
     */
    @PostMapping("/unlink")
    public ResponseEntity<?> unlink(@RequestBody UnlinkRequestDto unlinkRequest, HttpSession session) {
        try {
            Long requestingUserId = getUserIdFromSession(session);

            // AP로 보낼 데이터 생성
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("linkId", unlinkRequest.getLinkId());
            payload.put("requestingUserId", requestingUserId);

            // AP 서버에 해지 명령 전송
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_UNLINK, payload);
            JsonNode apResponse = tcpClientService.sendMessage(msg);

            // AP의 응답을 클라이언트에 그대로 전달
            return ResponseEntity.ok(apResponse);

        } catch (AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "연동 해지 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 요청 Body를 받기 위한 DTO
    @Getter @Setter
    static class UnlinkRequestDto {
        private Long linkId;
    }
    // =================================================================
    // ▲▲▲ 연동 해지 API 끝 ▲▲▲
    // =================================================================

    /**
     * 현재 사용자의 연동 상태를 조회하는 API
     */
    @GetMapping("/status")
    public ResponseEntity<?> getLinkStatus(HttpSession session) {
        try {
            Long userId = getUserIdFromSession(session);

            ObjectNode payload = objectMapper.createObjectNode().put("userId", userId);
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_STATUS, payload);
            JsonNode apRes = tcpClientService.sendMessage(msg);

            return ResponseEntity.ok(apRes);

        } catch (AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "상태 조회 중 오류: " + e.getMessage()));
        }
    }

    /**
     * 연동 신청 상세 정보 조회 (신청자 이름 등)
     */
    @GetMapping("/details/{linkId}")
    public ResponseEntity<?> getLinkDetails(@PathVariable("linkId") Long linkId) {
        try {
            ObjectNode payload = objectMapper.createObjectNode().put("linkId", linkId);
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_GET_DETAILS, payload);
            JsonNode apResponse = tcpClientService.sendMessage(msg);
            
            return ResponseEntity.ok(apResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "상세 정보 조회 중 오류 발생: " + e.getMessage()));
        }
    }
    
    /**
     * OCR 사전 검증 API
     * 파일만 받아서 OCR 처리 후 결과 텍스트만 반환
     */
    @PostMapping("/ocr-verify")
    public ResponseEntity<?> verifyOcr(@RequestParam("linkId") Long linkId, // ✅ 추가 (필수)
                                       @RequestPart("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("검증할 파일이 없습니다.");
            }
            if (linkId == null || linkId == 0L) {
                throw new IllegalArgumentException("linkId가 필요합니다.");
            }

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("linkId", linkId); // ✅ AP에서 필요
            ObjectNode f = payload.putObject("file");
            f.put("filename", file.getOriginalFilename());
            f.put("contentType", file.getContentType());
            f.put("base64", Base64.getEncoder().encodeToString(file.getBytes()));

            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_OCR_VERIFY, payload);
            JsonNode apResponse = tcpClientService.sendMessage(msg);

            return ResponseEntity.ok(Map.of("success", true, "data", apResponse));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @PostMapping("/init")
    public ResponseEntity<?> initLink(HttpSession session) {
        try {
            Long applicantUserId = getUserIdFromSession(session);
            ObjectNode payload = objectMapper.createObjectNode().put("applicantUserId", applicantUserId);
            TcpMessage msg = new TcpMessage(Command.SPOUSE_LINK_INIT, payload);
            JsonNode apRes = tcpClientService.sendMessage(msg);
            return ResponseEntity.ok(apRes);
        } catch (AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }


    // =================================================================
    // Helper Methods
    // =================================================================
    
    /**
     * 세션에서 사용자 ID를 안전하게 가져오는 헬퍼 메소드
     */
    private Long getUserIdFromSession(HttpSession session) throws AuthException {
        Object v = session.getAttribute("user");
        if (v == null) {
            throw new AuthException("로그인이 필요합니다.");
        }
        try {
            return (v instanceof Long) ? (Long) v
                    : (v instanceof Integer) ? ((Integer) v).longValue()
                    : Long.valueOf(v.toString());
        } catch (Exception e) {
            throw new AuthException("세션 사용자 정보 형식 오류");
        }
    }
    
    
    
    /**
     * 인증 관련 예외 처리를 위한 내부 클래스
     */
    private static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }
}