package com.example.memo.jpa.entity.company;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dc_contribution_batch")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DcContributionBatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "uploader_id", nullable = false)
    private CompanyManager uploader;

    private String fileName;
    private int totalRecords;
    private Long totalAmount;

    /** 납입예정일 */
    private LocalDate planDate;

    /** 검증 요약 */
    private Integer okCount;
    private Integer errorCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime uploadedAt;

    @Builder.Default
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DcContributionItem> items = new ArrayList<>();

    /**
     * 배치(파일)의 전체적인 처리 현황
     * DRAFT: 초안, VALIDATED: 검증완료, CONFIRMED: 확정, PROCESSING: 처리중, EXECUTED: 실행완료
     */
    public enum BatchStatus {
        DRAFT("초안"),
        VALIDATED("검증완료"),
        CONFIRMED("확정"),
        PROCESSING("처리중"), 
        EXECUTED("실행완료"),
        FAILED("실패"),
        CANCELED("취소");

        @Getter
        private final String displayName;

        BatchStatus(String displayName) {
            this.displayName = displayName;
        }
    }

    public void addItem(DcContributionItem item) {
        this.items.add(item);
        item.setBatch(this);
    }
}