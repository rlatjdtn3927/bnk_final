package com.example.memo.jpa.entity.irp;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bank_account")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BankAccount {
	
	@Id 
	@Column(name="acct_no", length=30)
    private String acctNo;

    @Column(name="acct_pwd", nullable=false)
    private String acctPwd; // 암호화/토큰화 저장

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(precision = 18, scale = 0, nullable=false)
    private BigDecimal balance;
    
    @PrePersist void prePersist() {
        if (balance == null) balance = BigDecimal.ZERO;
    }
}
