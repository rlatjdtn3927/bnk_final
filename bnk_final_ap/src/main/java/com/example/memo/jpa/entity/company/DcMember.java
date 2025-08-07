package com.example.memo.jpa.entity.company;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dc_member")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DcMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rrn", nullable = false)
    private String rrn; // 주민등록번호 (암호화)

    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "base_date")
    private LocalDate baseDate;

    @Column(name = "annual_salary")
    private Long annualSalary;

    @Column(name = "employment_type")
    private String employmentType;

    private String email;
    private String phone;

    @Column(name = "user_id")
    private String userId; // FK로 매핑 안함 (String ID)

    @Column(name = "company_id")
    private Long companyId; // 회사 ID
}
