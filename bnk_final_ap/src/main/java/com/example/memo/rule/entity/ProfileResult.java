package com.example.memo.rule.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profile_result")
@Getter @Setter
@NoArgsConstructor
public class ProfileResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_result_seq_gen")
    @SequenceGenerator(
        name = "profile_result_seq_gen",
        sequenceName = "PROFILE_RESULT_SEQ", // DB 시퀀스 이름
        allocationSize = 1
    )
    @Column(name = "RESULT_ID")
    private Long resultId;

    @Column(name = "USER_ID")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TYPE_ID")
    private ProfileType type;

    @Column(name = "TOTAL_SCORE")
    private Integer totalScore;

    @Column(name = "ANALYZED_AT", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime analyzedAt = LocalDateTime.now();
}

