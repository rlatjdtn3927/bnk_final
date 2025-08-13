package com.example.memo.admin.service;

import com.example.memo.admin.service.MenuItem;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private final Map<String, List<MenuItem>> registry = new LinkedHashMap<>();

    public MenuService() {
        registry.put("dashboard", Arrays.asList(
            new MenuItem("overview", "개요")
        ));

        registry.put("stats", Arrays.asList(
            new MenuItem("member-report", "가입자 리포트"),
            new MenuItem("sales-report", "판매 리포트"),
            new MenuItem("aum", "총 자산 규모 조회 (IRP/DC)"),
            new MenuItem("product-alloc", "상품별 운용비중")
        ));

        registry.put("customer", Arrays.asList(
            new MenuItem("list", "고객 목록"),
            new MenuItem("detail", "고객 상세")
        ));

        registry.put("review", Arrays.asList(
            new MenuItem("list", "후기 목록"),
            new MenuItem("moderation", "후기 관리")
        ));

        registry.put("account", Arrays.asList(
            new MenuItem("profile", "계정 정보")
        ));
    }

    public List<MenuItem> items(String category) {
        return registry.getOrDefault(category, Arrays.asList());
    }

    public String defaultPage(String category) {
        return items(category).stream()
                .findFirst()
                .map(MenuItem::getPath)
                .orElse("overview");
    }
}
