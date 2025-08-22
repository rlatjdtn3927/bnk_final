package com.example.memo.admin.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.memo.admin.dto.MonthlySeriesDto;
import com.example.memo.admin.projection.MonthlyCountView;
import com.example.memo.jpa.repository.company.DcAccountRepository;
import com.example.memo.jpa.repository.irp.IrpJoinRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {
	
	private final IrpJoinRepository irpJoinRepository;
	private final DcAccountRepository dcAccountRepository;

	public MonthlySeriesDto getMonthlySeries(String fromYm, String toYm) {
        List<String> labels = expandMonths(fromYm, toYm);

        Map<String, Long> irpMap = irpJoinRepository.countMonthly(fromYm, toYm)
            .stream().collect(Collectors.toMap(MonthlyCountView::getYm, MonthlyCountView::getCnt));

        // created_at 기준 집계 사용
        Map<String, Long> dcMap = dcAccountRepository.countMonthlyByCreatedAt(fromYm, toYm)
            .stream().collect(Collectors.toMap(MonthlyCountView::getYm, MonthlyCountView::getCnt));

        List<Long> irp = new ArrayList<>();
        List<Long> dc  = new ArrayList<>();
        for (String ym : labels) {
            irp.add(irpMap.getOrDefault(ym, 0L));
            dc.add(dcMap.getOrDefault(ym, 0L));
        }
        return new MonthlySeriesDto(labels, irp, dc);
    }
	
	private List<String> expandMonths(String fromYm, String toYm) {
        YearMonth from = YearMonth.parse(fromYm);
        YearMonth to   = YearMonth.parse(toYm);
        List<String> months = new ArrayList<>();
        for (YearMonth cur = from; !cur.isAfter(to); cur = cur.plusMonths(1)) {
            months.add(cur.toString()); // "YYYY-MM"
        }
        return months;
    }
}
