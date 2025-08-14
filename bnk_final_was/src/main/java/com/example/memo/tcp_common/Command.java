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
    ACCOUNT_REQUEST_GET_APPROVED,
    ACCOUNT_REQUEST_GET_REJECTED,
	ACCOUNT_GET_DETAILS,// 계좌 상세 조회
	CONTRIBUTION_BATCH_CREATE,
    CONTRIBUTION_BATCH_VALIDATE,    // 부담금 일괄 등록 검증 실행
    CONTRIBUTION_BATCH_CONFIRM,   
    CONTRIBUTION_BATCH_LIST,
    CONTRIBUTION_COMPANY_ACCOUNT_LIST, // 출금계좌 조회
    CONTRIBUTION_PAYABLE_ITEM_LIST,
    CONTRIBUTION_EXECUTE_ITEMS, // 선택 항목 입금 실행;
    
    
    /*IRP가입관련*/
    IRP_JOIN_CREATE_DRAFT,      // step1: 초안 생성
    IRP_JOIN_UPDATE_CONTRACT,   // step3-2: 계약정보 업데이트
    IRP_JOIN_UPDATE_PRODUCT,    // step4: 상품 저장
    IRP_JOIN_SAVE,
    IRP_JOIN_GET,
    IRP_JOIN_TAX_PURPOSE, 
    IRP_JOIN_RETIRED_PURPOSE, 
    IRP_JOIN_COMPLETE,			// 계좌개설/완료
    
 // 보유현황 탭
    SUMMARY_GET,                 // 총 평가액/수익률/입금합 등 요약
    TXN_LIST,                    // TRANSACTION_HISTORY 조회
    DEPOSIT_LIST,                // DEPOSIT_HISTORY 조회
    DEPOSIT_CREATE,              // 계좌 입금

    // 상품관리 탭
    PORTFOLIO_LIST,              // 보유상품 목록
    PORTFOLIO_CHANGE_PREVIEW,    // 보유상품 변경 시뮬(미리보기)
    PORTFOLIO_CHANGE_APPLY,      // 보유상품 변경 적용

    MATURITY_RESERVATION_LIST,   // 만기상품 변경예약 목록
    MATURITY_RESERVATION_APPLY,  // 만기상품 변경예약 등록

    PENDING_BUY_LIST,            // 매수예정상품 목록
    PENDING_BUY_UPSERT,          // 매수예정상품 등록/변경
    PENDING_BUY_HISTORY,         // 매수예정상품 변경내역

    DO_UPSERT,                   // 디폴트옵션 등록/변경
    DO_HISTORY                   // 디폴트옵션 변경내역

}