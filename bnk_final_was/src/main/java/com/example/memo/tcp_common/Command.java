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
    
    // ===== 보유현황 탭 =====
    SUMMARY_GET,              // SummaryHandler: 총 평가액/수익률/입금합/당일입금/운용수익 요약 조회

    TXN_LIST,                 // TransactionHandler: TRANSACTION_HISTORY 목록 조회

    DEPOSIT_LIST,             // DepositHandler: DEPOSIT_HISTORY 목록 조회
    DEPOSIT_CREATE,           // DepositHandler: 계좌 입금(즉시 반영)

    // ===== 상품관리 탭 (보유/변경) =====
    PORTFOLIO_LIST,           // PortfolioHandler: 보유상품 목록
    PORTFOLIO_CHANGE_PREVIEW, // PortfolioHandler: 보유상품 변경 미리보기(시뮬)
    PORTFOLIO_CHANGE_APPLY,   // PortfolioHandler: 보유상품 변경 적용(즉시 반영)

    // ===== 위저드 공통(등록/변경/만기) =====
    TRADE_PREREQ_CHECK,       // TradePrereqHandler: 로그인/IRP 계좌 존재/잔액 사전 체크

    PRODUCT_SEARCH,           // ProductHandler: 상품 검색(FUND/ETF/TDF/PRINCIPAL, q=검색어)
    DOCUMENT_LIST,            // DocumentHandler: 선택 상품의 서류 목록(FundDocument/PrincipalDocument)

    ALLOCATION_PREVIEW,       // AllocationHandler: 운용비율 합계 검증 및 금액/수량 미리보기
    ALLOCATION_APPLY,         // AllocationHandler: 배분 확정(거래/보유/잔액 즉시 반영)

    // ===== 매수예정 =====
    PENDING_BUY_LIST,         // PendingBuyHandler: 매수예정상품 목록
    PENDING_BUY_UPSERT,       // PendingBuyHandler: 매수예정 등록/변경
    PENDING_BUY_HISTORY,      // PendingBuyHandler: 매수예정 변경내역(날짜별)

    // ===== 만기예정 변경예약 =====
    MATURITY_RESERVATION_LIST,  // MaturityReservationHandler: 만기 예정 목록
    MATURITY_RESERVATION_APPLY, // MaturityReservationHandler: 만기 변경예약 등록/적용

    // ===== 디폴트옵션 =====
    DO_UPSERT,                // DefaultOptionHandler: 디폴트옵션 등록/변경
    DO_HISTORY,               // DefaultOptionHandler: 디폴트옵션 변경 이력
    
    
    /*관리자 크롤링 데이터 관련 명령어*/
    ADMIN_CRAWL_GRANT_FILE_DOWNLOAD,
    ADMIN_CRAWL_CHECK_FILE_TASK,
    ADMIN_CRAWL_CHECK_UPDATE;

}