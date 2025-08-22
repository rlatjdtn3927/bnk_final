package com.example.memo.purchase.change.rest_controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.purchase.change.dto.RetainHoldingsDto;
import com.example.memo.purchase.change.dto.SellLineDto;
import com.example.memo.purchase.reserve.dto.UserInfoDto;
import com.example.memo.purchase.trade_common.dto.ChangeValueDto;
import com.example.memo.tcp_common.Command;
import com.example.memo.tcp_common.TcpClientService;
import com.example.memo.tcp_common.TcpMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/retain-api/change")
@RequiredArgsConstructor
public class ChangeRetainAPIController {
	private final TcpClientService tcpClientService;
	private final ObjectMapper mapper;
	
    /** 상단 사용자/계좌 카드용 데이터 */
	@PostMapping("/user-info")
	public ResponseEntity<?> userInfo(HttpSession session) {
	    ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
	    String userName = (String) session.getAttribute("username");

	    // 세션에 값이 없을 때도 NPE 없이 안전하게
	    if (dto == null) {
	        UserInfoDto empty = UserInfoDto.builder()
	            .accountId("-")
	            .accountType("-")
	            .userName(userName != null ? userName : "-")
	            .riskGrade("-")
	            .riskGradeNum(0)
	            .build();
	        return ResponseEntity.ok(Map.of("userInfo", empty));
	    }

	    String riskText = dto.getRiskGrade();
	    int riskGradeNum;
	    switch (riskText != null ? riskText : "") {
	        case "안정형":   riskGradeNum = 5; break;
	        case "안전추구형": riskGradeNum = 4; break;
	        case "위험중립형": riskGradeNum = 3; break;
	        case "적극투자형": riskGradeNum = 2; break;
	        case "공격투자형": riskGradeNum = 1; break;
	        default:         riskGradeNum = 0; break; // 미정/없음
	    }

	    dto.setRiskGradeNum(riskGradeNum);
	    session.setAttribute("ChangeValueDto", dto);

	    UserInfoDto userInfo = UserInfoDto.builder()
	        .accountId(dto.getAccountId())
	        .accountType(dto.getAccountType())
	        .userName(userName != null ? userName : "-")
	        .riskGrade(riskText != null ? riskText : "-")
	        .riskGradeNum(riskGradeNum) // ★ 포함!
	        .build();

	    return ResponseEntity.ok(Map.of("userInfo", userInfo));
	}

    /** 보유/이미 선택된 상품 목록(운용비율 포함) */
	@PostMapping("/retain")
    public ResponseEntity<?> retainList(@RequestParam String accountType, @RequestParam String accountId) {
    	
    	RetainHoldingsDto dto = new RetainHoldingsDto(accountType,accountId);
    	JsonNode msg = mapper.valueToTree(dto);
    	TcpMessage message = new TcpMessage(Command.CHANGE_GET_HOLDINGS, msg);
    	JsonNode response = tcpClientService.sendMessage(message);
        System.out.println(response.toString()); //여기서 받아온 데이터 형식 확인하십쇼
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	/** 보유목록에서 선택 저장(교체) */
	@PostMapping("/save-selection")
	public ResponseEntity<?> saveSelection(@RequestBody SelectionReq req, HttpSession session) {
	    ChangeValueDto cv = getOrInit(session);
	    List<SellLineDto> normalized = normalize(req.getItems());
	    cv.setSellLines(normalized);
	    cv.setSellTotalAmount(sum(normalized));
	    session.setAttribute("ChangeValueDto", cv);
	    return ResponseEntity.ok(Map.of("ok", true, "count", normalized.size()));
	}

	/** 매도목록 화면 진입 시: 현재 선택 항목 조회 */
	@PostMapping("/sell-items")
	public ResponseEntity<?> sellItems(HttpSession session) {
	    ChangeValueDto cv = getOrInit(session);
	    return ResponseEntity.ok(Map.of("items", cv.getSellLines(), "total", cv.getSellTotalAmount()));
	}

	/** 매도목록에서 비율 업데이트(합계 재계산) */
	@PostMapping("/update-ratios")
	public ResponseEntity<?> updateRatios(@RequestBody SelectionReq req, HttpSession session) {
	    ChangeValueDto cv = getOrInit(session);
	    Map<String, SellLineDto> base = cv.getSellLines().stream()
	        .collect(Collectors.toMap(this::keyOf, s -> s));

	    for (SellLineDto in : normalize(req.getItems())) {
	        String k = keyOf(in);
	        SellLineDto cur = base.get(k);
	        if (cur != null) {
	            cur.setRatio(in.getRatio());
	            cur.setSellAmount(calc(cur.getAvailableAmount(), in.getRatio()));
	        }
	    }
	    List<SellLineDto> updated = new ArrayList<>(base.values());
	    cv.setSellLines(updated);
	    cv.setSellTotalAmount(sum(updated));
	    session.setAttribute("ChangeValueDto", cv);
	    return ResponseEntity.ok(Map.of("ok", true, "total", cv.getSellTotalAmount()));
	}

	/** 추가선택 화면: 보유 후보(이미 선택된 건 제외) */
	@PostMapping("/holdings-for-add")
	public ResponseEntity<?> holdingsForAdd(HttpSession session) {
	    ChangeValueDto cv = getOrInit(session);

	    JsonNode req = mapper.createObjectNode()
	        .put("accountType", cv.getAccountType())
	        .put("accountId",   cv.getAccountId());

	    JsonNode res = tcpClientService.sendMessage(new TcpMessage(Command.CHANGE_GET_HOLDINGS, req));
	    JsonNode root = res.has("data") ? res.get("data") : res;

	    List<SellLineDto> all = new ArrayList<>();

	    // 원리금
	    if (root.has("principalLedgers") && root.get("principalLedgers").isArray()) {
	        root.get("principalLedgers").forEach(n -> all.add(
	            SellLineDto.builder()
	                .itemType("PRINCIPAL")
	                .itemId(n.path("principalId").asText(null))
	                .name(n.path("productName").asText("정기예금"))
	                .availableAmount(big(n, "contractAmount"))
	                .ratio(0).sellAmount(BigDecimal.ZERO).build()
	        ));
	    }
	    // 펀드
	    if (root.has("fundHoldings") && root.get("fundHoldings").isArray()) {
	        root.get("fundHoldings").forEach(n -> all.add(
	            SellLineDto.builder()
	                .itemType("FUND")
	                .itemId(n.path("fundProductId").asText(null))
	                .name(n.path("productName").asText("펀드"))
	                .availableAmount(big(n, "valuationAmount"))
	                .ratio(0).sellAmount(BigDecimal.ZERO).build()
	        ));
	    }
	    // (있다면) 현금성
	    if (root.has("cashAsset")) {
	        JsonNode c = root.get("cashAsset");
	        BigDecimal avail = Optional.ofNullable(big(c, "amount"))
	                                   .orElseGet(() -> big(c, "valuationAmount"));
	        all.add(SellLineDto.builder()
	                .itemType("CASH").itemId("CASH").name("(미운용)현금성자산")
	                .availableAmount(nz(avail)).ratio(0).sellAmount(BigDecimal.ZERO).build());
	    }

	    // 이미 선택된 항목 제외
	    Set<String> selected = getOrInit(session).getSellLines()
	        .stream().map(this::keyOf).collect(Collectors.toSet());
	    List<SellLineDto> candidates = all.stream()
	        .filter(s -> !selected.contains(keyOf(s))).toList();

	    return ResponseEntity.ok(Map.of("items", candidates));
	}

	/** 추가선택(merge) */
	@PostMapping("/append-selection")
	public ResponseEntity<?> appendSelection(@RequestBody SelectionReq req, HttpSession session) {
	    ChangeValueDto cv = getOrInit(session);
	    Map<String, SellLineDto> map = cv.getSellLines().stream()
	        .collect(Collectors.toMap(this::keyOf, s -> s, (a,b)->a, LinkedHashMap::new));

	    for (SellLineDto s : normalize(req.getItems())) map.put(keyOf(s), s);

	    List<SellLineDto> merged = new ArrayList<>(map.values());
	    cv.setSellLines(merged);
	    cv.setSellTotalAmount(sum(merged));
	    session.setAttribute("ChangeValueDto", cv);
	    return ResponseEntity.ok(Map.of("ok", true, "count", merged.size()));
	}

	/* ====== 내부 유틸 & 요청 DTO ====== */

	private ChangeValueDto getOrInit(HttpSession session) {
	    ChangeValueDto dto = (ChangeValueDto) session.getAttribute("ChangeValueDto");
	    if (dto == null) dto = new ChangeValueDto();
	    return dto;
	}
	private String keyOf(SellLineDto s) { return s.getItemType() + "::" + s.getItemId(); }

	private List<SellLineDto> normalize(List<SellLineDto> in) {
	    if (in == null) return List.of();
	    return in.stream().map(s -> {
	        int r = s.getRatio() == null ? 0 : Math.max(0, Math.min(100, s.getRatio()));
	        BigDecimal avail = nz(s.getAvailableAmount());
	        return SellLineDto.builder()
	            .itemType(s.getItemType())
	            .itemId(s.getItemId())
	            .name(s.getName())
	            .availableAmount(avail)
	            .ratio(r)
	            .sellAmount(calc(avail, r))
	            .build();
	    }).toList();
	}
	private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
	private BigDecimal calc(BigDecimal avail, int ratio) {
	    return nz(avail).multiply(BigDecimal.valueOf(ratio))
	            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN); // 원단위 내림
	}
	private BigDecimal sum(List<SellLineDto> list) {
	    return list.stream().map(SellLineDto::getSellAmount)
	            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	private BigDecimal big(JsonNode n, String key) {
	    return n.hasNonNull(key) ? new BigDecimal(n.get(key).asText("0")) : null;
	}

	/** 요청 바디 래퍼 */
	@Data @NoArgsConstructor @AllArgsConstructor
	public static class SelectionReq {
	    private List<SellLineDto> items;
	}
}
