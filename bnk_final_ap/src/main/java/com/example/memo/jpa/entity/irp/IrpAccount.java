package com.example.memo.jpa.entity.irp;

import java.math.BigDecimal;

import com.example.memo.jpa.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "IRP_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrpAccount {
	
	@Id
    @Column(name = "irp_acct_no", length = 20)
    private String irpAcctNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable=false)
    private UserEntity user;

    @Column(precision = 18, scale = 0)
    private BigDecimal balance;
    
    @Column(name = "contract_no", nullable=false, unique=true, length=30)
    private String contractNo;
    
    @Column(name = "acct_pwd", length = 100)
    private String irpPwd; 
    
    @Column(length = 20)
    private String status;
    
    @PrePersist void prePersist() {
        if (balance == null) balance = BigDecimal.ZERO;
    }
}
