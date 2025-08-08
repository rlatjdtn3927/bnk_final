package com.example.memo.tcp_common;
/*여기 추가로 정의하세요*/
/*약속 커맨드 정의 시에 항상 도메인 이름 먼저 하고 언더바 뒤로는 알아서*/
public enum Command {
    USER_GET,
    USER_DELETE,
	
	COMPANY_REGISTER, // 기업 정보 등록
	COMPANY_LOGIN, // 기업 담당자 로그인
    SUBSCRIBER_VALIDATE, // 엑셀 데이터 검증용
    SUBSCRIBER_REGISTER, // 검증 완료된 데이터 등록용
	SUBSCRIBER_GET_LIST, // 가입자 등록 조회
	SUBSCRIBER_GET_LIST_SUCCESS, // 가입자 등로 성공
	BANK_EMPLOYEE_LOGIN,
    ACCOUNT_REQUEST_CREATE;

	
}