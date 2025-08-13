package com.example.memo.irp.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {
	
	private final JdbcTemplate jdbcTemplate;
	
	// YYYYMMDD(8자리) + LPAD(seq, 5, '0') = 총 13자리
    private static final String SQL =
        "SELECT TO_CHAR(SYSDATE,'YYYYMMDD') || LPAD(IRP_ACCT_NO_SEQ.NEXTVAL, 5, '0') FROM DUAL";

    public String next() {
        return jdbcTemplate.queryForObject(SQL, String.class);
    }
}
