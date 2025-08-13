package com.example.memo.jpa.entity.purchase.commodity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.example.memo.jpa.entity.BaseEntity;

@Entity
@Table(name = "do_specific")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DOSpecific extends BaseEntity{

    @Id
    @Column(name = "do_specific_id")
    private Long do_specific_id;  // Entity의 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "default_id", nullable = false)
    private DefaultOption defaultId;  // 디폴트 옵션 ID

    @Column(name = "cons_rate")
    private Integer consRate;  // 포트폴리오 내 비율

    @Column(name = "cons_name", length = 200)
    private String consName;  // 구성 상품 이름

    @Column(name = "cons_category", length = 50)
    private String consCategory;  // 상품 유형 (예: 정기예금, 수익증권 등)

    @Column(name = "offer_company", length = 100)
    private String offerCompany;  // 상품 제공 기관

    @Column(name = "simple_url", length = 500)
    private String simpleUrl;  // (펀드: 간이설명서)

    @Column(name = "desc_url", length = 500)
    private String descUrl;  //  상품설명서

    @Column(name = "terms_url", length = 500)
    private String termsUrl;  // 약관, 특약
}
