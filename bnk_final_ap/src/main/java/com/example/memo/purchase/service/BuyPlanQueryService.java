// AP - src/main/java/com/example/memo/purchase/service/BuyPlanQueryService.java
package com.example.memo.purchase.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.repository.trade.BuyPlanFundRepository;
import com.example.memo.jpa.repository.trade.BuyPlanPGRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuyPlanQueryService {

    private final BuyPlanFundRepository fundRepo;
    private final BuyPlanPGRepository   pgRepo;

    public List<Map<String,Object>> getCurrentPlan(Long userId){
        List<Map<String,Object>> out = new ArrayList<>();

        pgRepo.findByUser_User_idAndIsCurrent(userId, "Y").forEach(p -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("productId",   p.getPg().getProductId());
            m.put("productName", p.getPg().getProductName());
            m.put("type",        "PG");
            m.put("percent",     p.getAllocationPercent());
            out.add(m);
        });

        fundRepo.findByUser_User_idAndIsCurrent(userId, "Y").forEach(f -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("productId",   f.getFund().getProductId());
            m.put("productName", f.getFund().getProductName());
            m.put("type",        "FUND");
            m.put("percent",     f.getAllocationPercent());
            out.add(m);
        });

        return out;
    }
}
