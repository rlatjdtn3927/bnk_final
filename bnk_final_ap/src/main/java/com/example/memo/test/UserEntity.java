package com.example.memo.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_test_user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class UserEntity {
	
	@Id
	Integer userId;
	
	String password;
}
