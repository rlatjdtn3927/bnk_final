package com.example.memo.rule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profile_type")
@Getter @Setter
@NoArgsConstructor
public class ProfileType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TYPE_ID")   // DB 컬럼명 매핑
    private Long typeId;

    @Column(name = "TYPE_NAME") // DB 컬럼명 매핑
    private String typeName;

    @Column(name = "MIN_SCORE") // DB 컬럼명 매핑
    private Integer minScore;

    @Column(name = "MAX_SCORE") // DB 컬럼명 매핑
    private Integer maxScore;

    @Lob
    @Column(name = "DESCRIPTION") // DB 컬럼명 매핑
    private String description;
}
