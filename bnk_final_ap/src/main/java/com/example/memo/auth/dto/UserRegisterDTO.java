package com.example.memo.auth.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserRegisterDTO {
    private String username;
    private String nickname;   
    private String gender;    
    private String email;
    private String job;
    private LocalDate birthDate;
    private String password;   
}
