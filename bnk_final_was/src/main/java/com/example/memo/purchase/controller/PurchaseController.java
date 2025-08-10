package com.example.memo.purchase.controller;

import com.example.memo.purchase.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PurchaseController {

    /* 메인: 탭/레이아웃만 렌더, 콘텐츠는 비동기로 프래그먼트 로드 */
    @GetMapping("/purchase")
    public String purchaseHome() {
        return "purchase/index";
    }

    /* ====== 기존 프래그먼트 (유지) ====== */

    @GetMapping(value = "/purchase/fragment/planned", produces = MediaType.TEXT_HTML_VALUE)
    public String planned(Model model) {
        model.addAttribute("items", List.of(
                new PlannedItemDto("예금", "BNK e-쏠쏠 적금", 30, 300000, "월 적립"),
                new PlannedItemDto("펀드", "BNK 성장주식형", 40, 400000, "일시 매수"),
                new PlannedItemDto("TDF",  "BNK TDF 2045", 30, 300000, "리밸런싱 예정")
        ));
        return "purchase/_planned";
    }

    @GetMapping(value = "/purchase/fragment/holding", produces = MediaType.TEXT_HTML_VALUE)
    public String holding(Model model) {
        model.addAttribute("holdings", List.of(
                new HoldingItemDto("예금", "BNK 자유적금", 1200000, 2.1),
                new HoldingItemDto("펀드", "BNK 배당가치형", 850000, -1.3),
                new HoldingItemDto("ETF",  "KODEX 200", 560000, 5.4)
        ));
        return "purchase/_holding";
    }

    @GetMapping(value = "/purchase/fragment/maturity", produces = MediaType.TEXT_HTML_VALUE)
    public String maturity(Model model) {
        model.addAttribute("maturities", List.of(
                new MaturityItemDto("예금", "BNK 특판예금", LocalDate.now().plusDays(12), "자동재예치"),
                new MaturityItemDto("펀드", "BNK 중단기채", LocalDate.now().plusDays(45), "현금화 예정")
        ));
        return "purchase/_maturity";
    }

    @GetMapping(value = "/purchase/fragment/requests", produces = MediaType.TEXT_HTML_VALUE)
    public String requests(Model model) {
        model.addAttribute("requests", List.of(
                new RequestItemDto("보유상품 변경", "펀드 비중 30→40%", LocalDateTime.now().minusHours(2), "접수"),
                new RequestItemDto("만기상품 변경", "만기시 현금화", LocalDateTime.now().minusDays(1), "승인"),
                new RequestItemDto("매수예정 등록", "TDF 30%", LocalDateTime.now().minusDays(3), "취소")
        ));
        return "purchase/_requests";
    }

    @GetMapping(value = "/purchase/fragment/deposits", produces = MediaType.TEXT_HTML_VALUE)
    public String deposits(Model model) {
        model.addAttribute("account",
                new AccountSummaryDto("개인 IRP 홍길동", "123-45-678901", 5_000_000, 25.0));
        model.addAttribute("deposits", List.of(
                new DepositItemDto(LocalDateTime.now().minusDays(0), "입금", 100000),
                new DepositItemDto(LocalDateTime.now().minusDays(2), "입금", 300000),
                new DepositItemDto(LocalDateTime.now().minusDays(7), "출금", -50000)
        ));
        return "purchase/_deposits";
    }

    @GetMapping(value = "/purchase/fragment/tx", produces = MediaType.TEXT_HTML_VALUE)
    public String tx(Model model) {
        model.addAttribute("txs", List.of(
                new TxItemDto(LocalDateTime.now().minusDays(0), "ETF 매수", "KODEX 200", 2, 48830, 97660),
                new TxItemDto(LocalDateTime.now().minusDays(1), "펀드 매수", "배당가치형", 1, 100000, 100000),
                new TxItemDto(LocalDateTime.now().minusDays(3), "예금 가입", "자유적금", 1, 500000, 500000)
        ));
        return "purchase/_tx";
    }

    /* ====== 새 2레벨 탭 구조용 엔드포인트 (상품관리 / 보유현황 분리) ====== */

    /* --- 상품관리(Mgmt) --- */
    @GetMapping(value = "/purchase/fragment/mgmt/planned", produces = MediaType.TEXT_HTML_VALUE)
    public String mgmtPlanned(Model model) { return planned(model); }

    @GetMapping(value = "/purchase/fragment/mgmt/holding", produces = MediaType.TEXT_HTML_VALUE)
    public String mgmtHolding(Model model) { return holding(model); }

    @GetMapping(value = "/purchase/fragment/mgmt/maturity", produces = MediaType.TEXT_HTML_VALUE)
    public String mgmtMaturity(Model model) { return maturity(model); }

    @GetMapping(value = "/purchase/fragment/mgmt/requests", produces = MediaType.TEXT_HTML_VALUE)
    public String mgmtRequests(Model model) { return requests(model); }

    /* 디폴트옵션 등록/변경 (신규 프래그먼트: purchase/_default_option.html 필요) */
    @GetMapping(value = "/purchase/fragment/mgmt/default-option", produces = MediaType.TEXT_HTML_VALUE)
    public String mgmtDefaultOption(Model model) {
        model.addAttribute("defaults", List.of(
                Map.of("name", "BNK TDF 2045", "ratio", 60),
                Map.of("name", "BNK 중단기채",   "ratio", 40)
        ));
        return "purchase/_default_option";
    }

    /* --- 보유현황(Status) --- */
    /* 계좌 입금 (요약 + 입력 폼) – purchase/_deposit_form.html 필요 */
    @GetMapping(value = "/purchase/fragment/status/deposit-form", produces = MediaType.TEXT_HTML_VALUE)
    public String statusDepositForm(Model model) {
        model.addAttribute("account",
                new AccountSummaryDto("개인 IRP 홍길동", "123-45-678901", 5_000_000, 25.0));
        return "purchase/_deposit_form";
    }

    /* 계좌 입금내역 조회 – purchase/_deposit_history.html 필요 */
    @GetMapping(value = "/purchase/fragment/status/deposit-history", produces = MediaType.TEXT_HTML_VALUE)
    public String statusDepositHistory(Model model) {
        model.addAttribute("deposits", List.of(
                new DepositItemDto(LocalDateTime.now().minusDays(0), "입금", 100000),
                new DepositItemDto(LocalDateTime.now().minusDays(2), "입금", 300000),
                new DepositItemDto(LocalDateTime.now().minusDays(7), "출금", -50000)
        ));
        return "purchase/_deposit_history";
    }

    /* 거래내역 조회 – 기존 tx 재사용 */
    @GetMapping(value = "/purchase/fragment/status/tx", produces = MediaType.TEXT_HTML_VALUE)
    public String statusTx(Model model) { return tx(model); }
}
