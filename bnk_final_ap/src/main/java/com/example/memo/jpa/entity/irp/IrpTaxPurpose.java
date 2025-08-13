package com.example.memo.jpa.entity.irp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "IRP_tax_purpose")
@Getter
@Setter
@Builder
public class IrpTaxPurpose {
	
	@Id
	@Column(name="join_id")
	private Long joinId;
	
	@MapsId
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="join_id")
	private IrpJoinEntity join;
	
	@Column(name="irp_qual_type", length=20)
	private String irpQualType;		//자영업자, 근로자 등
	
	@Column(name="business_no", length=12)
	private String businessNo;		//사업자등록번호
}
