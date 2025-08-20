package com.example.memo.jpa.entity.irp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestUserEntity {
	
	@Id
	@Column(name = "user_id")
    private Long userId;

    private String name;

    private String email;
}
