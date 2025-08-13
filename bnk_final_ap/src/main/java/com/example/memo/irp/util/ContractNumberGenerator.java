package com.example.memo.irp.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractNumberGenerator {

	private final JdbcTemplate jdbcTemplate;
	
	// IRP + YYYYMMDD + 5자리 시퀀스
    private static final String SQL =
        "SELECT 'IRP' || TO_CHAR(SYSDATE,'YYYYMMDD') || LPAD(IRP_CONTRACT_NO_SEQ.NEXTVAL, 5, '0') FROM DUAL";

    public String next() {
        return jdbcTemplate.queryForObject(SQL, String.class);
    }
}
