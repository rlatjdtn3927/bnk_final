package com.example.memo.admin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.memo.admin.dto.stat.AdminStatsResponse;
import com.example.memo.admin.dto.stat.AllocationItem;
import com.example.memo.admin.dto.stat.RiskSplitItem;
import com.example.memo.admin.dto.stat.TotalAssetStat;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpAccountRepository;
import com.example.memo.purchase.change.dto.response.FundHoldingsDto;
import com.example.memo.purchase.change.dto.response.PrincipalLedgerDto;
import com.example.memo.purchase.change.dto.response.ResponseAccountHoldingsDto;
import com.example.memo.purchase.change.service.ChangeProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsAggService {

    private final ChangeProductService changeProductService;
    private final IrpAccountRepository irpRepo;
    private final DcAccountRepository dcRepo;
    private final ObjectMapper om;

    /** 한 번에 전체(총자산/상품비중/위험비중) 생성 */
    public AdminStatsResponse buildAdminStats() {
        // 1) 계좌번호 목록 + 현금 합계
        var irpIds = irpRepo.findAllAcctNos();
        var dcIds  = dcRepo.findAllAccountNos();
        BigDecimal irpCash = n(irpRepo.sumAllBalance());
        BigDecimal dcCash  = n(dcRepo.sumAllBalance());

        // 2) CHANGE_GET_HOLDINGS 기반 IRP/DC 합산
        PartialAgg irp = aggregateFor("IRP", irpIds);
        PartialAgg dc  = aggregateFor("DC",  dcIds);

        // 총자산용 현금 반영
        irp.cash = irpCash;
        dc.cash  = dcCash;

        // 3) 응답 DTO 구성
        Map<String, TotalAssetStat> totals = Map.of(
            "IRP", irp.toStat(),
            "DC",  dc.toStat()
        );
        var allocation = buildAllocation(irp, dc); // 현금 제외(원리금+펀드)
        var riskSplit  = buildRiskSplit(allocation);

        return new AdminStatsResponse(totals, allocation, riskSplit);
    }

    /** CHANGE_GET_HOLDINGS 호출 → 펀드/원리금 합산 (현금 제외) */
    private PartialAgg aggregateFor(String accountType, List<String> accountIds) {
        PartialAgg agg = new PartialAgg();
        for (String id : accountIds) {
            ObjectNode req = om.createObjectNode()
                               .put("accountType", accountType)
                               .put("accountId", id);

            ResponseAccountHoldingsDto dto = changeProductService.getHoldings(req);
            if (dto == null) continue;

            // 펀드(ETF/TDF/FUND) = 위험자산
            var funds = dto.getFundHoldings();
            if (funds != null) {
                for (FundHoldingsDto f : funds) {
                    BigDecimal val = n(f.getValuationAmount());
                    agg.fund = agg.fund.add(val);
                    String cat = lower(f.getCategory());
                    switch (cat) {
                        case "etf"  -> agg.etf = agg.etf.add(val);
                        case "tdf"  -> agg.tdf = agg.tdf.add(val);
                        case "fund" -> agg.fundEtc = agg.fundEtc.add(val);
                        default     -> agg.fundEtc = agg.fundEtc.add(val);
                    }
                }
            }

            // 원리금 = 안전자산
            var principals = dto.getPrincipalLedgers();
            if (principals != null) {
                for (PrincipalLedgerDto p : principals) {
                    BigDecimal amt = n(p.getContractAmount()).add(n(p.getInterestAccrued()));
                    agg.principal = agg.principal.add(amt);
                    agg.principalForAlloc = agg.principalForAlloc.add(amt);
                }
            }
        }
        return agg;
    }

    /** 상품 비중 (현금 제외) */
    private List<AllocationItem> buildAllocation(PartialAgg irp, PartialAgg dc) {
        BigDecimal principal = irp.principalForAlloc.add(dc.principalForAlloc);
        BigDecimal etf  = irp.etf.add(dc.etf);
        BigDecimal tdf  = irp.tdf.add(dc.tdf);
        BigDecimal fund = irp.fundEtc.add(dc.fundEtc);

        BigDecimal denom = principal.add(etf).add(tdf).add(fund);
        if (denom.signum() == 0) denom = BigDecimal.ONE;

        return List.of(
            new AllocationItem("PRINCIPAL", s(principal), pct(principal, denom)),
            new AllocationItem("ETF",        s(etf),       pct(etf, denom)),
            new AllocationItem("TDF",        s(tdf),       pct(tdf, denom)),
            new AllocationItem("FUND",       s(fund),      pct(fund, denom))
        );
    }

    /** 위험/안전 비중 (현금 제외) */
    private List<RiskSplitItem> buildRiskSplit(List<AllocationItem> alloc) {
        BigDecimal principal = alloc.stream()
            .filter(a -> "PRINCIPAL".equals(a.category()))
            .map(AllocationItem::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal riskAmt = alloc.stream()
            .filter(a -> !"PRINCIPAL".equals(a.category()))
            .map(AllocationItem::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal denom = principal.add(riskAmt);
        if (denom.signum() == 0) denom = BigDecimal.ONE;

        return List.of(
            new RiskSplitItem("SAFE", s(principal), pct(principal, denom)),
            new RiskSplitItem("RISK", s(riskAmt),   pct(riskAmt,   denom))
        );
    }

    /** 내부 누적용 구조체 */
    private static class PartialAgg {
        BigDecimal cash = BigDecimal.ZERO;       // 총자산용
        BigDecimal principal = BigDecimal.ZERO;  // 총자산용
        BigDecimal fund = BigDecimal.ZERO;       // 총자산용

        // 비중용(현금 제외)
        BigDecimal principalForAlloc = BigDecimal.ZERO;
        BigDecimal etf = BigDecimal.ZERO;
        BigDecimal tdf = BigDecimal.ZERO;
        BigDecimal fundEtc = BigDecimal.ZERO;

        TotalAssetStat toStat() {
            BigDecimal total = cash.add(principal).add(fund);
            return new TotalAssetStat(s(cash), s(principal), s(fund), s(total));
        }
    }

    // helpers
    private static BigDecimal n(BigDecimal v){ return v==null?BigDecimal.ZERO:v; }
    private static BigDecimal s(BigDecimal v){ return n(v).setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal pct(BigDecimal a, BigDecimal d){
        return n(a).multiply(BigDecimal.valueOf(100))
                   .divide(d, 2, RoundingMode.HALF_UP);
    }
    private static String lower(String s){ return s==null? "" : s.toLowerCase(); }
}