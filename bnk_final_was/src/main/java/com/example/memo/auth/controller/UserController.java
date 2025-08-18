package com.example.memo.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.memo.auth.dto.UserJoinDTO;

@RestController
@RequestMapping("/user-api")
public class UserController {
	
	  
	  @PostMapping("/register")
	  public ResponseEntity<?> register(@RequestBody UserJoinDTO dto) {
		  	
	
	        return ResponseEntity.ok("회원가입 성공! 로그인페이지로 이동합니다");
	    }
}
 