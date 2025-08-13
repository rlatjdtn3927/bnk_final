package com.example.memo.admin.service;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SidebarService {

    private final Map<String, List<Subcat>> byCat = new HashMap<>();
    private final Map<String, String> name = Map.of(
    	"dashboard", "대시보드",
        "stat", "통계",
        "sales", "판매 관리",
        "account", "계좌 개설 관리"
    );
    private final Map<String, String> def = new HashMap<>();

    public SidebarService() {
    	
    	byCat.put("dashboard", new ArrayList<>(Arrays.asList(
    		    Subcat.builder().code("dashboard").label("대시보드")
    		          .view("admin/pages/dashboard").category("dashboard").build()
    		)));
    	def.put("dashboard", "dashboard");
    		
    		
        byCat.put("stat", new ArrayList<>(Arrays.asList(
            Subcat.builder().code("stat-subscribers").label("가입자 리포트")
                  .view("admin/pages/stat/subscribers-report").category("stat").build()
        )));
        def.put("stat", "stat-subscribers");

        byCat.put("sales", new ArrayList<>(Arrays.asList(
            Subcat.builder().code("sales-dashboard").label("판매 대시보드")
                  .view("admin/pages/sales/sales-dashboard").category("sales").build()
        )));
        def.put("sales", "sales-dashboard");

        byCat.put("account", new ArrayList<>(Arrays.asList(
            Subcat.builder().code("account-open").label("계좌 개설")
                  .view("admin/pages/account/open-account").category("account").build()
        )));
        def.put("account", "account-open");

        // === 여기처럼 원하는 만큼 쉽게 추가 ===
         add("stat", "stat-test", "통계 테스트", "admin/pages/stat/stat-test");
        // setDefault("stat", "stat-test"); // 기본 첫 화면으로 바꾸고 싶으면
    }

    /* -------- 공개 API -------- */

    /** 카테고리에 서브카테고리 추가(가변 리스트 보장) */
    public void add(String category, String code, String label, String view) {
        byCat.computeIfAbsent(category, k -> new ArrayList<>())
             .add(Subcat.builder()
                       .code(code).label(label).view(view).category(category).build());
    }

    /** 카테고리 기본 진입 코드 지정 */
    public void setDefault(String category, String code) {
        def.put(category, code);
    }

    public List<Subcat> list(String cat) {
        return byCat.getOrDefault(cat, Collections.emptyList());
    }

    public String nameOf(String cat) {
        return name.getOrDefault(cat, cat);
    }

    /** 기본코드가 없으면 첫 번째 항목으로 폴백(null 방지) */
    public String defaultOf(String cat) {
        String d = def.get(cat);
        if (d != null) return d;
        return byCat.getOrDefault(cat, Collections.emptyList())
                    .stream().findFirst().map(Subcat::getCode).orElse(null);
    }

    public Optional<Subcat> find(String code) {
        return byCat.values().stream().flatMap(List::stream)
                    .filter(s -> s.getCode().equals(code)).findFirst();
    }

    public String categoryOf(String code) {
        return find(code).map(Subcat::getCategory).orElse("stat");
    }
}
