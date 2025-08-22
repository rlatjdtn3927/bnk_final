package com.example.memo.jpa.entity.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="company_account")
@Getter @Setter
public class CompanyAccount {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
    @Column(name = "account_no", nullable = false, unique=true)
    private String accountNo;

    @Column(name = "bank", nullable = false)
    private String bank;

    @Column(name = "balance", nullable = false)
    private String balance;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
