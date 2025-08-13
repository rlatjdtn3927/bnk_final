package com.example.memo.tcp_common;
/*여기 추가로 정의하세요*/
public enum Command {
    USER_GET,
    USER_DELETE,

    EMBEDDING_CREATE, // 문서 청크 생성 및 임베딩 요청
    CHAT_ASK_QUESTION, // 질문에 대한 응답 요청
    BRANCH_NEARBY, // 현재 위치 기준, 부산은행 지점 Top5

	
	COMPANY_REGISTER, // 기업 정보 등록
	COMPANY_LOGIN, // 기업 담당자 로그인
    SUBSCRIBER_VALIDATE, // 엑셀 데이터 검증용
    SUBSCRIBER_REGISTER, // 검증 완료된 데이터 등록용
	SUBSCRIBER_GET_LIST, // 가입자 등록 조회
	SUBSCRIBER_GET_LIST_SUCCESS, // 가입자 등로 성공
	BANK_EMPLOYEE_LOGIN, // 은행 직원(관리자) 로그인
    ACCOUNT_REQUEST_CREATE, // DC 계좌 개설 요청
    ACCOUNT_REQUEST_GET_PENDING,  // 승인 대기중인 요청 목록 조회
    ACCOUNT_REQUEST_APPROVE_BULK,  // 일괄 승인
    ACCOUNT_REQUEST_REJECT_BULK,   // 일괄 거절
	ACCOUNT_GET_DETAILS,// 계좌 상세 조회
	CONTRIBUTION_BATCH_CREATE,
    CONTRIBUTION_BATCH_VALIDATE,    // 부담금 일괄 등록 검증 실행
    CONTRIBUTION_BATCH_CONFIRM,   
    CONTRIBUTION_BATCH_LIST,
    CONTRIBUTION_COMPANY_ACCOUNT_LIST, // 출금계좌 조회
    CONTRIBUTION_BATCH_EXECUTE,          // 선택 배치 입금 실행
    CONTRIBUTION_PAYABLE_ITEM_LIST,
    CONTRIBUTION_EXECUTE_ITEMS // 선택 항목 입금 실행;

}