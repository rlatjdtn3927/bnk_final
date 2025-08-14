package com.example.memo.admin.service;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SidebarService {

    private final Map<String, List<Subcat>> byCat = new HashMap<>();
    private final Map<String, String> name = Map.of(
    	"dashboard", "대시보드",
        "stat", "통계",
        "commodity", "상품 관리",
        "customers", "고객 관리",
        "review", "리뷰 관리",
        "account", "계좌 개설 관리"
    );
    private final Map<String, String> def = new HashMap<>();

    public SidebarService() {
    	
    	byCat.put("dashboard", new ArrayList<>(Arrays.asList(
    		    Subcat.builder().code("dashboard").label("대시보드")
    		          .view("admin/pages/dashboard/dashboard").category("dashboard").build()
    		)));
    	def.put("dashboard", "dashboard");
    		
    		
        byCat.put("stat", new ArrayList<>(Arrays.asList(
            Subcat.builder().code("stat-subscribers").label("가입자 리포트")
                  .view("admin/pages/stat/subscribers-report").category("stat").build()
        )));
        def.put("stat", "stat-subscribers");
        
        byCat.put("commodity", new ArrayList<>(Arrays.asList(
                Subcat.builder().code("commodity-management").label("상품 관리")
                      .view("admin/pages/commodity/commodity-management").category("commodity").build()
        )));
        def.put("commodity", "commodity-management");

        byCat.put("customers", new ArrayList<>(Arrays.asList(
            Subcat.builder().code("customers-management").label("고객 관리")
                  .view("admin/pages/customers/customers-management").category("customers").build()
        )));
        def.put("customers", "customers-management");
        
        byCat.put("review", new ArrayList<>(Arrays.asList(
                Subcat.builder().code("review-management").label("후기 관리")
                      .view("admin/pages/review/review-management").category("review").build()
            )));
        def.put("review", "review-management");

        byCat.put("account", new ArrayList<>(Arrays.asList(
            Subcat.builder().code("account-open").label("계좌 개설")
                  .view("admin/pages/account/open-account").category("account").build()
        )));
        def.put("account", "account-open");

        // === 서브 카테고리 추가 추가 ===
//        예시 : public void add(String category, String code, String label, String view)
          // category: 상위 카테고리
          // code: 페이지 구별하는 식별자. URL이나 hx-get 호출 시 사용
          // label: 사이드바에 보이는 글자
          // view: 템플릿 경로
         add("stat", "sales-report", "판매 리포트", "admin/pages/stat/sales-report");
         add("stat", "total-assets", "총 자산 규모 조회(IRP/DC)", "admin/pages/stat/total-assets-report");
         add("stat", "product-allocation", "상품별 운용비중", "admin/pages/stat/product-allocation-ratio");
        // setDefault("stat", "stat-test"); // 기본 첫 화면으로 바꾸고 싶으면
         
         add("commodity", "crawling-management", "크롤링 관리", "admin/pages/commodity/crawling-management");
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
