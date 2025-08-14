package com.example.memo.jpa.entity.purchase.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.memo.jpa.entity.BaseEntity;
import com.example.memo.jpa.entity.purchase.common.AccountType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deposit_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DepositHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_id")
    private Long depositId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 10, nullable = false)
    private AccountType accountType; // IRP / DC

    @Column(name = "acount_id", nullable = false, length = 30)
    private String acountId;

    @Column(name = "deposit_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "balance_after", precision = 15, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "deposit_date", nullable = false)
    private LocalDateTime depositDate;
}
