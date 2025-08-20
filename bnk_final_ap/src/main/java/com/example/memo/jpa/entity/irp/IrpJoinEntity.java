package com.example.memo.jpa.entity.irp;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "IRP_join")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IrpJoinEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Oracle 12c+ IDENTITY
    @Column(name="join_id")
	private Long joinId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)	// FK(user_account.user_id)
	private UserEntity user;
	
	//가입목적
    @Column(name="join_purpose", nullable=false, length=20)
	private String joinPurpose;	// 세액공제, 퇴직금수령, 계약이전
	
	//연 납입 한도 금액
    @Column(name="annual_contrib_amt")
    private Long annualContribAmt;
    
    //신규 입금 금액
    @Column(name="new_contrib_amt")
    private Long newContribAmt;
    
    //계약번호
    @Column(name="contract_no", unique=true, length=30)
    private String contractNo;	//IRP계좌 계약번호와 연결

    //출금계좌번호 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acct_no")
    private BankAccount acctNo;
    
    //IRP계좌번호 FK
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "irp_acct_no", unique = true)
    private IrpAccount irpAccount;
    
    //관리영업점
    @Column(name="branch_office", length=50)
    private String branchOffice;

    @Column(name="reg_date", nullable=false)
    private LocalDate regDate;
    
    @PrePersist void prePersist(){ if(regDate==null) regDate = LocalDate.now(); }
}
