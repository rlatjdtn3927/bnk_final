package com.example.memo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BnkFinalApApplication {

	public static void main(String[] args) {
		SpringApplication.run(BnkFinalApApplication.class, args);
	}

}
