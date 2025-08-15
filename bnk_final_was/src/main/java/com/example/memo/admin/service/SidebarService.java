package com.example.memo.admin.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class SidebarService {

	/** 카테고리 -> 서브카테고리 목록 */
    private final Map<String, List<Subcat>> byCat = new LinkedHashMap<>();
    /** 카테고리 표시 이름 (표시용 라벨) */
    private final Map<String, String> name = new LinkedHashMap<>();
    /** 카테고리 기본 진입 코드 */
    private final Map<String, String> def = new LinkedHashMap<>();

    /** code -> Subcat 빠른 조회 */
    private final Map<String, Subcat> byCode = new LinkedHashMap<>();
    
    public SidebarService() {
        // 표시 이름 등록 (원하는 라벨)
        name.put("dashboard",  "대시보드");
        name.put("stat",       "통계");
        name.put("commodity",  "상품 관리");
        name.put("customers",  "고객 관리");
        name.put("review",     "리뷰 관리");
        name.put("account",    "계좌 개설 관리");
    }
    

    @PostConstruct
    public void init() {
        // register(카테고리, 코드, 라벨, 뷰 경로, 기본값 여부)
        register("dashboard", "dashboard", "대시보드", "admin/pages/dashboard/dashboard", true);
        
        register("stat", "stat-subscribers", "가입자 리포트", "admin/pages/stat/subscribers-report", true);
        register("stat", "sales-report", "판매 리포트", "admin/pages/stat/sales-report", false);
        register("stat", "total-assets", "총 자산 규모 조회(IRP/DC)", "admin/pages/stat/total-assets-report", false);
        register("stat", "product-allocation", "상품별 운용비중", "admin/pages/stat/product-allocation-ratio", false);

        register("commodity", "commodity-management", "상품 관리", "admin/pages/commodity/commodity-management", true);
        register("commodity", "crawling-management", "크롤링 관리", "admin/pages/commodity/crawling-management", false);

        register("customers", "customers-management", "고객 관리", "admin/pages/customers/customers-management", true);
        
        register("review", "review-management", "후기 관리", "admin/pages/review/review-management", true);

        register("account", "account-open", "계좌 개설", "admin/pages/account/open-account", true);
    }
    
    /**
     * 메뉴 정보를 각 Map에 등록하는 private 헬퍼 메소드
     */
    private void register(String category, String code, String label, String view, boolean isDefault) {
        Subcat subcat = Subcat.builder()
                              .code(code).label(label).view(view).category(category).build();

        // 카테고리별 리스트에 추가
        byCat.computeIfAbsent(category, k -> new ArrayList<>()).add(subcat);
        // 코드별 조회용 Map에 추가
        byCode.put(code, subcat);

        // 기본값으로 지정된 경우 def Map에 추가
        if (isDefault) {
            def.put(category, code);
        }
    }

    /* -------- 공개 API (Public Methods) -------- */
    // 공개 API는 기존의 add, setDefault를 register를 호출하도록 변경하거나, private으로 만들고 init()에서만 사용하도록 할 수 있습니다.
    // 여기서는 외부에서 동적 추가가 필요할 수 있으니 public으로 유지

    /** 카테고리에 서브카테고리 추가 */
    public void add(String category, String code, String label, String view) {
        // 기본값이 아닌 메뉴로 등록
        register(category, code, label, view, false);
    }

    /** 카테고리 기본 진입 코드 지정 */
    public void setDefault(String category, String code) {
        // 해당 코드가 존재하는지 먼저 확인하는 로직을 추가하면 더 안전합니다.
        if (byCode.containsKey(code)) {
            def.put(category, code);
        }
    }

    public List<Subcat> list(String cat) {
        return byCat.getOrDefault(cat, Collections.emptyList());
    }

    public String nameOf(String cat) {
        return name.getOrDefault(cat, cat);
    }

    public String defaultOf(String cat) {
        String d = def.get(cat);
        if (d != null) return d;
        return byCat.getOrDefault(cat, Collections.emptyList())
                    .stream().findFirst().map(Subcat::getCode).orElse(null);
    }

    public Optional<Subcat> find(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public String categoryOf(String code) {
        return find(code).map(Subcat::getCategory).orElse(null);
    }

    /* ------------------- Header ------------------- */

    /** 헤더 카테고리 DTO(record) */
    public record HeaderCategory(String categoryCode, String categoryName, String defaultPageCode) {}

    /**
     * 헤더 카테고리를 **고정된 순서**로 반환
     * 원하는 순서: 대시보드 → 통계 → 상품 관리 → 고객 관리 → 리뷰 관리 → 계좌 개설 관리
     */
    public List<HeaderCategory> getHeaderCategories() {
        List<String> order = List.of("dashboard", "stat", "commodity", "customers", "review", "account");

        return order.stream()
                // 존재하고 기본 페이지가 정의된 카테고리만
                .filter(cat -> def.get(cat) != null)
                .map(cat -> new HeaderCategory(
                        cat,
                        name.getOrDefault(cat, cat),
                        def.get(cat)
                ))
                .toList();
    }
}