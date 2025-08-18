package com.example.memo.jpa.entity.purchase.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.memo.jpa.entity.BaseEntity;
import com.example.memo.jpa.entity.purchase.common.AccountType;
import com.example.memo.jpa.entity.purchase.common.TxnType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransactionHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Long txnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 10, nullable = false)
    private AccountType accountType; // IRP / DC

    @Column(name = "acount_id", nullable = false, length = 30)
    private String acountId;

    @Column(name = "product_id", length = 30, nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", length = 20, nullable = false)
    private TxnType txnType; // BUY / SELL

    @Column(name = "txn_amt", precision = 15, scale = 0)
    private BigDecimal txnAmt;

    @Column(name = "quantity", precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;
}
