package com.example.memo.jpa.entity.company;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dc_member_status")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DcMemberStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "retire_date")
    private LocalDate retireDate;

    @Column(name = "cancel_date")
    private LocalDate cancelDate;

    @Column(name = "first_pay_date")
    private LocalDate firstPayDate;

    @Column(name = "db_ratio")
    private Double dbRatio;

    @Column(name = "dc_ratio")
    private Double dcRatio;

    @Column(name = "dc_member_id")
    private Long dcMemberId; // FK to dc_member(id)
}
