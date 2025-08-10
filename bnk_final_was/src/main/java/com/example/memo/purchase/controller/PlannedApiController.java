package com.example.memo.purchase.controller;

import com.example.memo.purchase.dto.PlannedItemDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 매수예정상품 등록/변경 – 비동기 API (세션 메모리 저장)
 * DB/엔티티 없이 작동. AP 연동 시 여기서 TCP로 위임하면 됨.
 */
@RestController
@RequestMapping("/api/purchase/planned")
@RequiredArgsConstructor
public class PlannedApiController {

    // ------------ 세션 상태 ------------
    private static final String KEY = "PLANNED_STATE";

    private SessionState state(HttpSession session) {
        SessionState st = (SessionState) session.getAttribute(KEY);
        if (st == null) {
            st = SessionState.seed(); // 초기값 심기
            session.setAttribute(KEY, st);
        }
        return st;
    }

    // ------------ 조회 ------------
    @GetMapping("/state")
    public ResponseEntity<?> getState(@RequestParam("mode") String mode, HttpSession session) {
        var s = state(session).byMode(mode);
        return ok(Map.of(
                "mode", mode,
                "items", s.now,
                "before", s.before,
                "totalRatio", s.now.stream().mapToInt(PlannedItemDto::getRatio).sum()
        ));
    }

    @GetMapping("/products")
    public ResponseEntity<?> products(@RequestParam("category") String category) {
        List<ProductDto> list = ProductDto.seed().stream()
                .filter(p -> p.getCategory().equals(category))
                .collect(Collectors.toList());
        return ok(Map.of("category", category, "products", list));
    }

    // ------------ 편성 변경 ------------
    @PostMapping("/change")
    public ResponseEntity<?> change(@RequestBody ChangeReq req, HttpSession session) {
        var m = state(session).byMode(req.getMode());

        switch (req.getOp()) {
            case "add": {
                require(req.getProduct() != null, "상품을 선택하세요.");
                require(req.getRatio() != null && req.getRatio() > 0 && req.getRatio() <= 100, "비율은 1~100");
                PlannedItemDto it = new PlannedItemDto(
                        req.getProduct().getCategory(),
                        req.getProduct().getName(),
                        req.getRatio(),
                        0,
                        req.getProduct().getNote()
                );
                it.setId(nextId());
                m.now.add(it);
                break;
            }
            case "replaceAll": {
                require(req.getProduct() != null, "상품을 선택하세요.");
                require(req.getRatio() != null && req.getRatio() > 0 && req.getRatio() <= 100, "비율은 1~100");
                m.now.clear();
                PlannedItemDto it = new PlannedItemDto(
                        req.getProduct().getCategory(),
                        req.getProduct().getName(),
                        req.getRatio(),
                        0,
                        req.getProduct().getNote()
                );
                it.setId(nextId());
                m.now.add(it);
                break;
            }
            case "updateRatio": {
                require(req.getIndex() != null, "index 필요");
                requireInRange(req.getIndex(), 0, m.now.size()-1, "index 범위 오류");
                require(req.getRatio() != null && req.getRatio() >= 0 && req.getRatio() <= 100, "비율 0~100");
                m.now.get(req.getIndex()).setRatio(req.getRatio());
                break;
            }
            case "updateAmount": {
                require(req.getIndex() != null, "index 필요");
                requireInRange(req.getIndex(), 0, m.now.size()-1, "index 범위 오류");
                m.now.get(req.getIndex()).setAmount(Optional.ofNullable(req.getAmount()).orElse(0));
                break;
            }
            case "remove": {
                require(req.getIndex() != null, "index 필요");
                requireInRange(req.getIndex(), 0, m.now.size()-1, "index 범위 오류");
                m.now.remove((int) req.getIndex());
                break;
            }
            default:
                return bad("지원하지 않는 op");
        }
        return ok(Map.of(
                "items", m.now,
                "totalRatio", m.now.stream().mapToInt(PlannedItemDto::getRatio).sum()
        ));
    }

    // ------------ 단계 전환 ------------
    @PostMapping("/next")
    public ResponseEntity<?> next(@RequestBody ModeReq req, HttpSession session) {
        var m = state(session).byMode(req.getMode());
        int sum = m.now.stream().mapToInt(PlannedItemDto::getRatio).sum();
        if (sum != 100) return bad("비율 합계가 100%여야 합니다.");
        return ok(Map.of("ok", true));
    }

    @PostMapping("/agreements")
    public ResponseEntity<?> agreements(@RequestBody AgreementReq req) {
        if (!(req.isSummary() && req.isTerms() && req.isProspectus())) {
            return bad("모든 서류에 동의해야 합니다.");
        }
        return ok(Map.of("ok", true));
    }

    @GetMapping("/review")
    public ResponseEntity<?> review(@RequestParam("mode") String mode, HttpSession session) {
        var m = state(session).byMode(mode);
        return ok(Map.of("before", m.before, "after", m.now,
                "totalAfter", m.now.stream().mapToInt(PlannedItemDto::getRatio).sum()));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody ModeReq req, HttpSession session) {
        var m = state(session).byMode(req.getMode());
        int sum = m.now.stream().mapToInt(PlannedItemDto::getRatio).sum();
        if (sum != 100) return bad("비율 합계가 100%여야 합니다.");
        // now -> before 반영
        m.before = deepCopy(m.now);
        return ok(Map.of("ok", true, "message", "등록(또는 변경) 완료"));
    }

    // ------------ 유틸 ------------
    private static ResponseEntity<?> ok(Object body) { return ResponseEntity.ok(body); }
    private static ResponseEntity<?> bad(String msg) { return ResponseEntity.badRequest().body(Map.of("error", msg)); }
    private static void require(boolean cond, String msg){ if(!cond) throw new IllegalArgumentException(msg); }
    private static void requireInRange(int v, int min, int max, String msg){
        if(v<min || v>max) throw new IllegalArgumentException(msg);
    }
    private static List<PlannedItemDto> deepCopy(List<PlannedItemDto> src){
        return src.stream().map(i-> new PlannedItemDto(i.getCategory(), i.getName(), i.getRatio(), i.getAmount(), i.getNote()){
            { setId(i.getId()); }
        }).collect(Collectors.toList());
    }
    private static final AtomicLong SEQ = new AtomicLong(1000);
    private static long nextId(){ return SEQ.incrementAndGet(); }

    // ------------ 세션 모델 ------------
    @Data
    public static class SessionState {
        private ModeState irp;
        private ModeState dc;

        ModeState byMode(String mode){
            if("IRP".equalsIgnoreCase(mode)) return irp;
            if("DC".equalsIgnoreCase(mode)) return dc;
            throw new IllegalArgumentException("mode는 IRP|DC");
        }

        static SessionState seed(){
            SessionState s = new SessionState();
            s.irp = new ModeState();
            s.dc  = new ModeState();

            s.irp.before = List.of(
                    item("예금","BNK 자유적금",20,200000,"월 적립"),
                    item("펀드","BNK 배당가치형",40,400000,"일시 매수"),
                    item("TDF","BNK TDF 2045",40,400000,"장기")
            );
            s.irp.now = new ArrayList<>(List.of(
                    item("예금","BNK e-쏠쏠 적금",30,300000,"월 적립"),
                    item("펀드","BNK 성장주식형",40,400000,"일시 매수"),
                    item("TDF","BNK TDF 2045",30,300000,"리밸런싱 예정")
            ));
            s.dc.before = List.of(
                    item("예금","BNK 특판예금",50,500000,"만기 자동재예치"),
                    item("펀드","BNK 중단기채",50,500000,"안정추구")
            );
            s.dc.now = new ArrayList<>(List.of(
                    item("예금","BNK 자유적금",50,500000,"월 적립"),
                    item("펀드","BNK 중단기채",50,500000,"안정추구")
            ));
            return s;
        }

        static PlannedItemDto item(String c, String n, int r, int a, String note){
            PlannedItemDto d = new PlannedItemDto(c,n,r,a,note);
            d.setId(nextId());
            return d;
        }
    }

    @Data
    public static class ModeState {
        private List<PlannedItemDto> before = new ArrayList<>();
        private List<PlannedItemDto> now = new ArrayList<>();
    }

    // ------------ DTOs ------------
    @Data
    public static class ProductDto {
        private String id;
        private String category;
        private String name;
        private String note;

        public ProductDto(String id, String category, String name, String note) {
            this.id = id; this.category = category; this.name = name; this.note = note;
        }
        public static List<ProductDto> seed(){
            return List.of(
                    new ProductDto("p1","예금","BNK 자유적금","월 적립"),
                    new ProductDto("p2","예금","BNK 특판예금","기간 한정"),
                    new ProductDto("p3","TDF","BNK TDF 2045","장기"),
                    new ProductDto("p4","TDF","BNK TDF 2050","초장기"),
                    new ProductDto("p5","펀드","BNK 성장주식형","성장주"),
                    new ProductDto("p6","펀드","BNK 배당가치형","배당/가치"),
                    new ProductDto("p7","ETF","KODEX 200","인덱스"),
                    new ProductDto("p8","ETF","TIGER 미국채10년","채권"),
                    new ProductDto("p9","현금성자산","MMF(단기)","현금성")
            );
        }
    }

    @Data public static class ChangeReq {
        private String mode;      // IRP|DC
        private String op;        // add|replaceAll|updateRatio|updateAmount|remove
        private Integer index;    // 일부 op에서 사용
        private Integer ratio;    // add/replaceAll/updateRatio
        private Integer amount;   // updateAmount
        private ProductDto product; // add/replaceAll
    }

    @Data public static class AgreementReq {
        private String mode; // IRP|DC
        private boolean summary;
        private boolean terms;
        private boolean prospectus;
    }

    @Data public static class ModeReq {
        private String mode; // IRP|DC
    }
}

