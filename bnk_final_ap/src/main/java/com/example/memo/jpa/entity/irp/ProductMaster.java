package com.example.memo.jpa.entity.irp;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_master")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductMaster {
	@Id 
	@Column(name="product_id", length=30)
    private String productId;

	@Column(name = "product_type")
    private String productType; // FUND / GUARANTEE / DEFAULT

    @Column(name="product_name", nullable=false)
    private String productName;

    @Column(name="reg_date", nullable=false)
    private LocalDate regDate;

    @PrePersist void prePersist(){ if(regDate==null) regDate = LocalDate.now(); }
}
