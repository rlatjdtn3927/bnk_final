package com.example.memo.jpa.entity.purchase.commodity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

import com.example.memo.jpa.entity.BaseEntity;

@Entity
@Table(name = "principal_guarantee")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrincipalGuarantee extends BaseEntity{

    @Id
    @Column(name = "product_id", length = 30)
    private String productId;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "maturity_years")
    private String maturityYears;

    @Column(name = "db_rate", precision = 5, scale = 2)
    private BigDecimal dbRate;

    @Column(name = "dc_rate", precision = 5, scale = 2)
    private BigDecimal dcRate;

    @Column(name = "irp_rate", precision = 5, scale = 2)
    private BigDecimal irpRate;

    @Column(name = "status", length = 20)
    private String status;  // 예: "PENDING", "INSERTED", "UPDATED"

}