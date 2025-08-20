package com.example.memo.jpa.entity.company;

import java.time.LocalDate;

import com.example.memo.jpa.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // user_account.user_id FK 참조
    private UserEntity user;


    @Column(name = "company_id")
    private Long companyId; // 회사 ID
    
    @OneToOne(mappedBy = "dcMember", fetch = FetchType.LAZY)
    private DcMemberStatus dcMemberStatus;

    public void setDcMemberStatus(DcMemberStatus dcMemberStatus) {
        this.dcMemberStatus = dcMemberStatus;
        // 반대편에도 관계를 설정하여 양방향 관계의 일관성을 맞춘다.
        if (dcMemberStatus != null && dcMemberStatus.getDcMember() != this) {
            dcMemberStatus.setDcMember(this);
        }
    }
}
