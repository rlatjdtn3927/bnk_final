package com.example.memo.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com.example.memo.company.service.CryptoService;
import com.example.memo.jpa.entity.purchase.commodity_pending.FundDocumentPending;
import com.example.memo.jpa.entity.purchase.commodity_pending.FundMasterPending;
import com.example.memo.jpa.entity.purchase.commodity_pending.PrincipalDocumentPending;
import com.example.memo.jpa.entity.purchase.commodity_pending.PrincipalGuaranteePending;
import com.example.memo.jpa.repository.purchase.analysis.CumulativePerformanceRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundDocumentRepository;
import com.example.memo.jpa.repository.purchase.analysis.FundStatusRepository;
import com.example.memo.jpa.repository.purchase.analysis.PrincipalDocumentRepository;
import com.example.memo.jpa.repository.purchase.analysis_pending.AssetAllocationPendingRepository;
import com.example.memo.jpa.repository.purchase.analysis_pending.CumulativePerformancePendingRepository;
import com.example.memo.jpa.repository.purchase.analysis_pending.FundNavPendingRepository;
import com.example.memo.jpa.repository.purchase.analysis_pending.FundReturnPendingRepository;
import com.example.memo.jpa.repository.purchase.analysis_pending.FundStatusPendingRepository;
import com.example.memo.jpa.repository.purchase.analysis_pending.RiskMetricPendingRepository;
import com.example.memo.jpa.repository.purchase.analysis_pending.Top5SectorPendingRepository;
import com.example.memo.jpa.repository.purchase.commodity.FundMasterRepository;
import com.example.memo.jpa.repository.purchase.commodity.PrincipalGuaranteeRepository;
import com.example.memo.jpa.repository.purchase.commodity_pending.FundDocumentPendingRepository;
import com.example.memo.jpa.repository.purchase.commodity_pending.FundMasterPendingRepository;
import com.example.memo.jpa.repository.purchase.commodity_pending.PrincipalDocumentPendingRepository;
import com.example.memo.jpa.repository.purchase.commodity_pending.PrincipalGuaranteePendingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final CryptoService cryptoService;

    
    private final FundStatusRepository fundStatusRepository;
    private final CumulativePerformanceRepository cumulativePerformanceRepository;

    

    private final FundMasterRepository fundMasterRepository;
    private final PrincipalGuaranteeRepository principalGuaranteeRepository;
    private final PrincipalDocumentRepository principalDocumentRepository;
    private final FundDocumentRepository fundDocumentRepository;
    


}
