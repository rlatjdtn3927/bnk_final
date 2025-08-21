package com.example.memo.jpa.entity.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_account") // 실제 테이블명
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq_gen")
    @SequenceGenerator(name = "user_seq_gen", sequenceName = "USER_SEQ", allocationSize = 1)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 15)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;  // 자바 변수명은 camelCase

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "gender", nullable = false, columnDefinition = "CHAR(1)")
    private String gender;

    @Column(name = "rrn", nullable = false)
    private String rrn; // 주민등록번호

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "job", nullable = false, length = 50)
    private String job;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime created_at;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated_at;
}
