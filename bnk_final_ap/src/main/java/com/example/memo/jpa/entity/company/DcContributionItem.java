package com.example.memo.jpa.entity.company;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dc_contribution_item")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DcContributionItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "batch_id", nullable = false)
    private DcContributionBatch batch;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dc_member_id")
    private DcMember dcMember;

    private Long amount;

    // --- 데이터 검증 단계의 결과 ---
    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus; // SUCCESS / FAIL
    private String errorMessage;
    private String errorCode;

    // --- 실제 입금 처리 단계의 결과 ---
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    /**
     * 데이터의 유효성 검증 결과
     */
    public enum ValidationStatus {
        SUCCESS, FAIL
    }

    /**
     * 개별 항목의 실제 입금 처리 상태
     * PENDING: 입금 대기, PAID: 입금 완료, SKIPPED: 입금 제외, FAILED: 입금 실패
     */
    public enum PaymentStatus {
        PENDING("입금대기"),
        PAID("입금완료"),
        SKIPPED("입금제외"),
        FAILED("입금실패");

        @Getter
        private final String displayName;

        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }
    }
}