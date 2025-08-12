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

    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus; // SUCCESS / FAIL

    private String errorMessage;
    private String errorCode; // 선택(프론트 요약/필터용)

    public enum ValidationStatus { SUCCESS, FAIL }
}