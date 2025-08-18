// src/main/java/com/example/memo/purchase/service/ProductService.java
package com.example.memo.purchase.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.commodity.FundMaster;
import com.example.memo.jpa.entity.purchase.commodity.PrincipalGuarantee;
import com.example.memo.jpa.repository.purchase.commodity.FundMasterRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;

/**
 * 상품 검색 서비스
 * - category: FUND / ETF / TDF / PRINCIPAL / CASH
 * - q(선택): 상품명/코드 키워드
 *
 * 응답: [{ productId, productName, category }]
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ObjectMapper om;
    private final FundMasterRepository fundRepo;
    private final PrincipalGuaranteeRepository principalRepo;

    public JsonNode searchProducts(JsonNode req) {
        final String category = req.path("category").asText("").trim().toUpperCase(Locale.ROOT);
        final String qRaw     = Optional.ofNullable(req.path("q").asText(null)).orElse("").trim();

        ArrayNode out = om.createArrayNode();

        switch (category) {
            case "FUND":
            case "ETF":
            case "TDF": {
                // ★ 오직 fund_master.category 로만 구분
                List<FundMaster> list;
                if (qRaw.isEmpty()) {
                    list = fundRepo.findByCategoryIgnoreCase(category);
                } else {
                    // 이름/코드 결과를 병합하여 productId 기준 중복 제거
                    List<FundMaster> byName = fundRepo
                            .findByCategoryIgnoreCaseAndProductNameContainingIgnoreCase(category, qRaw);
                    List<FundMaster> byId = fundRepo
                            .findByCategoryIgnoreCaseAndProductIdContainingIgnoreCase(category, qRaw);
                    list = mergeByIdAndSortByName(byName, byId);
                }

                for (FundMaster f : list) {
                    out.add(om.createObjectNode()
                            .put("productId",   f.getProductId())
                            .put("productName", f.getProductName())
                            .put("category",    category));
                }
                break;
            }

            case "PRINCIPAL": {
                List<PrincipalGuarantee> list;
                if (qRaw.isEmpty()) {
                    // 기존 searchByQ(q="")가 전체 조회였으므로 동일 동작 보장
                    list = principalRepo.findAll();
                } else {
                    List<PrincipalGuarantee> byName = principalRepo.findByProductNameContainingIgnoreCase(qRaw);
                    List<PrincipalGuarantee> byId   = principalRepo.findByProductIdContainingIgnoreCase(qRaw);
                    list = mergeByIdAndSortByNamePG(byName, byId);
                }

                for (PrincipalGuarantee p : list) {
                    out.add(om.createObjectNode()
                            .put("productId",   p.getProductId())
                            .put("productName", p.getProductName())
                            .put("category",    "PRINCIPAL"));
                }
                break;
            }

            case "CASH":
                // 현금성은 별도 카탈로그 없음 → 화면에서 '현금성 추가' 버튼 로직으로 처리
                break;

            default:
                // 알 수 없는 카테고리 → 빈 배열
                break;
        }
        return out;
    }

    /** FundMaster 리스트 두 개를 productId 기준으로 병합 + 이름순 정렬 */
    private static List<FundMaster> mergeByIdAndSortByName(List<FundMaster> a, List<FundMaster> b) {
        Map<String, FundMaster> merged = new LinkedHashMap<>();
        if (a != null) a.forEach(f -> merged.putIfAbsent(f.getProductId(), f));
        if (b != null) b.forEach(f -> merged.putIfAbsent(f.getProductId(), f));
        return merged.values().stream()
                .sorted(Comparator.comparing(FundMaster::getProductName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /** PrincipalGuarantee 리스트 두 개를 productId 기준으로 병합 + 이름순 정렬 */
    private static List<PrincipalGuarantee> mergeByIdAndSortByNamePG(
            List<PrincipalGuarantee> a, List<PrincipalGuarantee> b) {
        Map<String, PrincipalGuarantee> merged = new LinkedHashMap<>();
        if (a != null) a.forEach(p -> merged.putIfAbsent(p.getProductId(), p));
        if (b != null) b.forEach(p -> merged.putIfAbsent(p.getProductId(), p));
        return merged.values().stream()
                .sorted(Comparator.comparing(PrincipalGuarantee::getProductName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }
}
