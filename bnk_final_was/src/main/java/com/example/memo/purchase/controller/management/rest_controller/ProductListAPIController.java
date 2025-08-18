package com.example.memo.purchase.controller.management.rest_controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product-api/list")
public class ProductListAPIController {
    public record ProductListReq(Integer riskGradeNum, String sort) {}

    // ---------------- 정기예금 ----------------
    @PostMapping("/deposit")
    public List<Map<String, Object>> deposit(@RequestBody(required = false) ProductListReq req) {
        List<Map<String, Object>> data = new ArrayList<>();

        data.add(deposit("DEP-IRP-001", "SBI저축은행퇴직연금정기예금(개인형IRP) 1년제", 3.0));
        data.add(deposit("DEP-IRP-002", "대신저축은행퇴직연금정기예금(개인형IRP) 1년제", 3.2));
        data.add(deposit("DEP-IRP-003", "OK저축은행퇴직연금정기예금(개인형IRP) 1년제", 3.1));

        // 금리 정렬 옵션
        if (req != null && "rate".equalsIgnoreCase(req.sort())) {
            data = data.stream()
                    .sorted((a,b) -> Double.compare(num(b.get("interestRate")), num(a.get("interestRate"))))
                    .collect(Collectors.toList());
        }
        return data;
    }

    private Map<String, Object> deposit(String id, String name, double rate) {
        return new HashMap<>() {{
            put("productId", id);
            put("productName", name);
            put("interestRate", rate);   // 뷰가 사용하는 필드
            // 보장을 뷰에서 chip으로 표현(정기예금이면 보장)
            put("riskGradeText", "원리금보장");
        }};
    }

    // ---------------- TDF ----------------
    @PostMapping("/tdf")
    public List<Map<String, Object>> tdf(@RequestBody(required = false) ProductListReq req) {
        List<Map<String, Object>> data = List.of(
                fundLike("TDF-2055-A", "신한BNPP TDF2055(퇴직연금)", 0.74, 10.15, 14.85, "중간", "TDF 2055"),
                fundLike("TDF-2045-A", "미래에셋 TDF2045(퇴직연금)", 0.62, 8.40, 12.10, "중간", "TDF 2045"),
                fundLike("TDF-2030-A", "KB TDF2030(퇴직연금)", 0.58, 5.20, 9.35, "중간", "TDF 2030")
        );
        return sortFundLike(data, req);
    }

    // ---------------- 펀드 ----------------
    @PostMapping("/fund")
    public List<Map<String, Object>> fund(@RequestBody(required = false) ProductListReq req) {
        List<Map<String, Object>> data = List.of(
                fundLike("FUND-001", "한국투신 초이스주식형(퇴직연금)", 0.89, 12.3, 21.7, "높음", "주식형"),
                fundLike("FUND-002", "삼성 혼합채권형(퇴직연금)",   0.45, 2.1,  5.4,  "중간", "채권혼합형"),
                fundLike("FUND-003", "NH 고배당가치주(퇴직연금)",   0.70, 6.0,  14.2, "높음", "주식형")
        );
        return sortFundLike(data, req);
    }

    // ---------------- ETF ----------------
    @PostMapping("/etf")
    public List<Map<String, Object>> etf(@RequestBody(required = false) ProductListReq req) {
        List<Map<String, Object>> data = List.of(
                fundLike("ETF-001", "KODEX 200TR(퇴직연금)", 0.15, 11.0, 18.4, "중간", "국내주식ETF"),
                fundLike("ETF-002", "TIGER S&P500(퇴직연금)", 0.07, 13.2, 23.6, "중간", "해외주식ETF"),
                fundLike("ETF-003", "KINDEX 미국채10년(퇴직연금)", 0.09, 3.1,  7.5,  "낮음", "해외채권ETF")
        );
        return sortFundLike(data, req);
    }

    // 공통: 펀드/TDF/ETF 카드에 필요한 필드 shape
    private Map<String, Object> fundLike(
            String id, String name, double totalFee, double ret3m, double ret1y,
            String riskText, String fundType
    ) {
        return new HashMap<>() {{
            put("productId", id);
            put("productName", name);
            put("totalFee", totalFee);     // 총보수(%)
            put("return3m", ret3m);        // 수익률(3개월)
            put("return1y", ret1y);        // 수익률(1년)
            put("riskGradeText", riskText);
            put("fundType", fundType);
        }};
    }

    private List<Map<String, Object>> sortFundLike(List<Map<String, Object>> data, ProductListReq req) {
        if (req == null || req.sort() == null) return data;

        return switch (req.sort()) {
            case "ret3m" -> data.stream()
                    .sorted((a,b) -> Double.compare(num(b.get("return3m")), num(a.get("return3m"))))
                    .collect(Collectors.toList());
            case "ret1y" -> data.stream()
                    .sorted((a,b) -> Double.compare(num(b.get("return1y")), num(a.get("return1y"))))
                    .collect(Collectors.toList());
            default -> data; // "default" 또는 미지정: 정렬 없음
        };
    }

    private double num(Object o) {
        if (o == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(o)); }
        catch (Exception e) { return 0.0; }
    }
	
}
