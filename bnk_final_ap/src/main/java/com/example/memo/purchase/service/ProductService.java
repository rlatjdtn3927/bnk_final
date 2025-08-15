// src/main/java/com/example/memo/purchase/service/ProductService.java
package com.example.memo.purchase.service;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.repository.purchase.commodity.FundMasterRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;

/**
 * 상품 검색
 * - FUND/ETF/TDF: FundMaster 대상(여기서는 모두 FUND 취급, 필요시 type 필드로 구분 가능)
 * - PRINCIPAL: 원리금보장 상품
 * - CASH: 검색 불필요(버튼으로 추가)
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ObjectMapper om;
    private final FundMasterRepository fundRepo;
    private final PrincipalGuaranteeRepository principalRepo;

    public JsonNode searchProducts(JsonNode req){
        String category = req.path("category").asText();
        String q = req.path("q").asText("");
        ArrayNode arr = om.createArrayNode();

        switch (category.toUpperCase()){
            case "FUND": case "ETF": case "TDF":
                fundRepo.findByProductNameContainingIgnoreCase(q).forEach(f->{
                    arr.add(om.createObjectNode()
                            .put("category","FUND") // 화면에서 FUND/ETF/TDF는 동일 취급
                            .put("productId", f.getProductId())
                            .put("productName", f.getProductName())
                            .put("meta", f.getTotalExpenseRatio()==null? "-" : ("총보수 " + f.getTotalExpenseRatio().toPlainString()+"%")));
                });
                break;
            case "PRINCIPAL":
                principalRepo.findByProductNameContainingIgnoreCase(q).forEach(p->{
                    arr.add(om.createObjectNode()
                            .put("category","PRINCIPAL")
                            .put("productId", p.getProductId())
                            .put("productName", p.getProductName())
                            .put("meta", p.getIrpRate()==null? "-" : ("금리 " + p.getIrpRate().toPlainString()+"%")));
                });
                break;
            case "CASH":
                // 검색 없음
                break;
        }
        return arr;
    }
}
