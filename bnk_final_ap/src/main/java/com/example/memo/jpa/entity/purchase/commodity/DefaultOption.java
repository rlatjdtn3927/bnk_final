package com.example.memo.jpa.entity.purchase.commodity;

import com.example.memo.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "default_option")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class DefaultOption extends BaseEntity{

    @Id
    @Column(name = "default_id")
    private String defaultId;

    @Column(name = "option_name", length = 100)
    private String optionName;

    @Column(name = "risk_grade_num")
    private Integer riskGradeNum;

    @Column(name = "risk_grade_text", length = 20)
    private String riskGradeText;

    @Column(name = "sub_prod1",length = 50)
    private String subProd1;

    @Column(name = "sub_prod2",length = 50)
    private String subProd2;

    @Column(name = "desc_url",length = 500)
    private String descUrl;

    @Column(name = "guide_url",length = 500)
    private String guideUrl;

    private Integer stblRate;

    private Integer nvstRate;
}