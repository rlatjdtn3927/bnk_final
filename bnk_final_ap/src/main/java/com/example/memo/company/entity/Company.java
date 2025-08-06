package com.example.memo.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name="biz_no", length=12, nullable=false, unique=true)
    private String bizNo;
    
    @Column(name = "name", nullable = false)
    private String name; // 기업명

    @Column(name = "ceo_name", nullable = false)
    private String ceoName; // 대표자명

    @Column(name = "phone", nullable = false)
    private String phone; // 연락처

    @Column(name = "email", nullable = false)
    private String email; // 이메일

    @Column(name = "address", nullable = false)
    private String address;
    
}
