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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity // 테이블과 매핑할 클래스
@Table(name = "user_account") // 실제 테이블 이름
@AllArgsConstructor//모든 필드를 포함한 생성자 자동생성 
@NoArgsConstructor // jpa는 내부적으로 기본 생성자를 필요필수
@Getter// 이건암 ㅇㅇㅋ 메소드 자동으로 만들어주는거 ㅇㅇ
@Setter // 이것도암 ㅋ
public class User {

   @Id
   @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq_gen")
   @SequenceGenerator(name = "user_seq_gen", sequenceName = "USER_SEQ", allocationSize = 1)
   @Column(name = "user_id", nullable = false)

    private Long user_id;

    @Column(name = "username", nullable = false, unique = true, length = 15)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String password_hash;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "gender", nullable = false, columnDefinition = "CHAR(1)")
    private String gender;

    @Column(name = "rrn", nullable = false, length = 13)
    private String rrn; //주민번호

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "job", nullable = false, length = 50)
    private String job;
    

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate; //생일추가


    @Column(name = "created_at", nullable = false)
    private LocalDateTime created_at;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated_at;




}
