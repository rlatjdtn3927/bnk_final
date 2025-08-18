// src/main/java/com/example/memo/purchase/service/ProductService.java
package com.example.memo.purchase.service;

import java.util.List;
import java.util.Locale;

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
 * 응답 통일: [{ productId, productName, category, meta? }]
 *  - meta: 표시용 부가정보(필요하면 채워서 내려주면 됨)
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ObjectMapper om;
    private final FundMasterRepository fundRepo;
    private final PrincipalGuaranteeRepository principalRepo;

    public JsonNode searchProducts(JsonNode req) {
        String category = req.path("category").asText("").toUpperCase(Locale.ROOT);
        String q        = req.path("q").asText("");
        String qLike    = "%" + q.toUpperCase(Locale.ROOT) + "%";

        ArrayNode out = om.createArrayNode();

        switch (category) {
            case "FUND":
            case "ETF":
            case "TDF": {
                // FundMaster 한 테이블에서 운용하므로
                // 1) category 컬럼이 있다면 그 값(FUND/ETF/TDF)으로 필터
                // 2) 없다면 productId prefix(F/E/T)로 보조 필터
                boolean useCategoryColumn = hasFundCategoryColumn();
                String prefix = ("FUND".equals(category) ? "F%" :
                                ("ETF".equals(category)  ? "E%" : "T%"));
                List<FundMaster> list = fundRepo.searchByCategoryOrPrefix(
                        useCategoryColumn, category, prefix, q, qLike);
                list.forEach(f -> out.add(om.createObjectNode()
                        .put("productId",   f.getProductId())
                        .put("productName", f.getProductName())
                        .put("category",    category)
                        // .put("meta",     makeFundMeta(f)) // 필요시 주석 해제
                ));
                break;
            }
            case "PRINCIPAL": {
                List<PrincipalGuarantee> list = principalRepo.searchByQ(q, qLike);
                list.forEach(p -> out.add(om.createObjectNode()
                        .put("productId",   p.getProductId())
                        .put("productName", p.getProductName())
                        .put("category",    "PRINCIPAL")
                        // .put("meta",     makePrincipalMeta(p))
                ));
                break;
            }
            case "CASH": {
                // 현금성은 목록이 별도로 존재하지 않음 → 화면에서 '현금성 추가' 버튼으로 처리
                break;
            }
            default:
                // 알 수 없는 카테고리 → 빈 목록
                break;
        }
        return out;
    }

    /**
     * FundMaster에 category 컬럼을 실제로 사용할지 결정.
     * - 프로젝트 스키마에 category가 있으면 true, 없으면 false.
     * - 필요하다면 환경변수/설정값으로 분기 가능.
     */
    private boolean hasFundCategoryColumn() {
        return true; // 스키마에 category 없으면 false 로 바꾸세요.
    }

    // private String makeFundMeta(FundMaster f){ ... }
    // private String makePrincipalMeta(PrincipalGuarantee p){ ... }
}
