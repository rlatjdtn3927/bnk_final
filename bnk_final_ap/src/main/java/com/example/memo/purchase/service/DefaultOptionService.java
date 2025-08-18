// src/main/java/com/example/memo/purchase/service/DefaultOptionService.java
package com.example.memo.purchase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class DefaultOptionService {
    private final ObjectMapper om;
    public JsonNode upsert(JsonNode req){
        ObjectNode out = om.createObjectNode();
        out.put("status","OK");
        return out;
    }
    public JsonNode history(JsonNode req){ return om.createArrayNode(); }
}
