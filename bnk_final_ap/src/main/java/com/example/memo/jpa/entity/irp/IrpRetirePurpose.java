package com.example.memo.jpa.entity.irp;

import java.time.LocalDate;

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
@Table(name = "IRP_retire_purpose")
@Getter
@Setter
@Builder
public class IrpRetirePurpose {
	@Id 
	@Column(name="join_id")
    private Long joinId;

    @MapsId
    @OneToOne(fetch=FetchType.LAZY) 
    @JoinColumn(name="join_id")
    private IrpJoinEntity join;

    @Column(name="retire_date", nullable=false)
    private LocalDate retireDate;

    @Column(name="retire_reason")
    private String retireReason;

    @Column(name="corp_name", length=100)
    private String corpName;

    @Column(name="severance_amt")
    private Long severanceAmt;

    @Column(name="withhold_doc")
    private String withholdDoc;
}
