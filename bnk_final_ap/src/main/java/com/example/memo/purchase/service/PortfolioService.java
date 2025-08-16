// src/main/java/com/example/memo/purchase/service/PortfolioService.java
package com.example.memo.purchase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class PortfolioService {
    private final ObjectMapper om;

    public JsonNode list(JsonNode req){ return om.createArrayNode(); }

    public JsonNode changePreview(JsonNode req){
        ObjectNode out = om.createObjectNode();
        out.put("status","PREVIEW");
        return out;
    }

    @Transactional
    public JsonNode changeApply(JsonNode req){
        ObjectNode out = om.createObjectNode();
        out.put("status","APPLIED");
        return out;
    }
}
