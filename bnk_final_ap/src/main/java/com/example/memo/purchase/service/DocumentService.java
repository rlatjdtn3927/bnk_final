// src/main/java/com/example/memo/purchase/service/DocumentService.java
package com.example.memo.purchase.service;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.repository.purchase.analysis.FundDocumentRepository;
import com.example.memo.jpa.repository.purchase.analysis.PrincipalDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;

/**
 * 상품별 동의 서류 목록
 * - FUND/ETF/TDF → FundDocument
 * - PRINCIPAL    → PrincipalDocument
 * - CASH         → 동의 없음
 * 프런트는 체크박스로 전부 동의되었는지 검증 후 다음 단계로 이동한다.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final ObjectMapper om;
    private final FundDocumentRepository fundDocRepo;
    private final PrincipalDocumentRepository principalDocRepo;

    public JsonNode listDocuments(JsonNode req){
        ArrayNode out = om.createArrayNode();
        for(JsonNode it : req.withArray("items")){
            String type = it.path("productType").asText();
            String pid  = it.path("productId").asText();
            switch (type){
                case "FUND": case "ETF": case "TDF":
                    fundDocRepo.findByFund_ProductId(pid).forEach(d ->
                            out.add(om.createObjectNode()
                                    .put("docId", d.getDocId())
                                    .put("docType", d.getDocType())
                                    .put("fileUrl", d.getFileUrl())));
                    break;
                case "PRINCIPAL":
                    principalDocRepo.findByPrincipal_ProductId(pid).forEach(d ->
                            out.add(om.createObjectNode()
                                    .put("docId", d.getDocId())
                                    .put("docType", d.getDocType())
                                    .put("fileUrl", d.getFileUrl())));
                    break;
                default: // CASH
                    break;
            }
        }
        return out;
    }
}
