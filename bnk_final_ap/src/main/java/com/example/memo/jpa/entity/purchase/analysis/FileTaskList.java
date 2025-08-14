package com.example.memo.jpa.entity.purchase.analysis;

import com.example.memo.jpa.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_task_list")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileTaskList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long task_id;

    @Column(name="prod_id")
    private String prodId;

    @Column(name="prod_category", length = 30)
    private String prodCategory; //fund, etf, tdf, 원리금보장상품

    @Column(name="doc_type" ,length = 50)
    private String docType; // 투자설명서, 상품약관, 간이 투자 설명서 //// 약관, 상품설명서

    @Column(name="download_url", length = 300)
    private String downloadUrl;

    @Column
    private Long fileSize;
}

