package com.example.memo.jpa.entity.rule;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "question")
@Getter @Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    // number -> questionNo 로 변경 (예약어 충돌 방지)
    @Column(name = "QUESTION_NO")
    private Integer questionNo;

    @Lob
    private String text;

    private String part;

    @Column(name = "SCORE_MAX")
    private Integer scoreMax;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OptionChoice> options = new ArrayList<>();
}
