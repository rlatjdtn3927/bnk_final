package com.example.memo.irp.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {
	
	private final JdbcTemplate jdbcTemplate;
	
	private static final String SQL_NEXT_ACCT_NO =
		    "SELECT 'IRP-' || TO_CHAR(SYSDATE,'YYYYMMDD') || '-' || LPAD(IRP_ACCT_NO_SEQ.NEXTVAL, 6, '0') FROM DUAL";

		public String next() {
		    return jdbcTemplate.queryForObject(SQL_NEXT_ACCT_NO, String.class);
		}
}
