package com.example.memo.irp.entity;

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
@Table(name = "IRP_transfer_purpose")
@Getter
@Setter
@Builder
public class IrpTransferPurpose {
	@Id 
	@Column(name="join_id")
    private Long joinId;

    @MapsId
    @OneToOne(fetch=FetchType.LAZY) 
    @JoinColumn(name="join_id")
    private IrpJoinEntity join;

    @Column(name="prev_bank", nullable=false, length=20)
    private String prevBank;

    @Column(name="prev_account", nullable=false, length=30)
    private String prevAccount;

    @Column(name="transfer_amt", nullable=false)
    private Long transferAmt;

    @Column(name="transfer_doc", length=255)
    private String transferDoc;
}
