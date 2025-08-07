package com.example.memo.tcp_common;


/*여기 추가로 정의하세요*/
public enum Command {
    USER_GET,
    USER_DELETE,
	
	COMPANY_REGISTER, // 기업 정보 등록
	COMPANY_LOGIN, // 기업 담당자 로그인
    SUBSCRIBER_VALIDATE, // 엑셀 데이터 검증용
    SUBSCRIBER_REGISTER; // 검증 완료된 데이터 등록용
}