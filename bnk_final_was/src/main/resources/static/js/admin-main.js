//로그아웃 기능
function logout() {
	fetch('/member/logout')
		.then(response => response.text())
		.then(data => {
			alert('로그아웃 되었습니다!');
			location.href = 'loginForm'
		})
		.catch(err => {
			alert('오류발생!(문의:(내선) 6241)');
		});
}

//DOM 요소 설정
const topMenu = document.querySelector('.topMenu'); // HTML class="topMenu"
const sidebarMenu = document.getElementById('sidebarMenu'); // HTML id="sidebarMenu"  
const mainContentArea = document.getElementById('main-content-area'); // HTML id="main-content-area"

// 소분류 매뉴 배열.
const submenus = {
	'approval': ['결재 문서 조회', '결재 대기 목록'],
	'notice': ['공지사항 목록', '공지사항 등록'],
	'product': ['상품 목록', '상품 등록'],
	'report': ['일일 보고서', '주간 보고서', '월간 보고서'],
	'management': ['사용자 목록', '사용자 등록', '사용자 권한 변경']
};

//공지사항 조회 - 순차 번호로 수정
function showNoticeList() {
	console.log("공지사항 목록 조회 시작");

	fetch('/notice/getNoticeList')
		.then(response => {
			console.log("공지사항 목록 응답 상태:", response.status);
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			console.log("공지사항 목록 데이터:", data);

			if (!Array.isArray(data)) {
				console.error("받은 데이터가 배열이 아닙니다:", typeof data);
				throw new Error("서버에서 잘못된 형식의 데이터를 받았습니다.");
			}

			console.log("공지사항 개수:", data.length);

			//인덱스 표시
			let tableRows = data.map((notice, index) => {
				const sequentialNumber = index + 1;
				return `
                    <tr>
                        <td>${sequentialNumber}</td>
                        <td>
                            <a href="#" onclick="viewNoticeContent(${notice.b_id})" 
                               style="color: #007bff; text-decoration: none;">
                                ${notice.b_title || '제목 없음'}
                            </a>
                        </td>
                        <td>${notice.b_view || 0}</td>
                        <td>
                            <button onclick="viewNoticeContent(${notice.b_id})" 
                                    style="padding: 5px 10px; border: 1px solid #007bff; 
                                           background: #f56565; color: white; border-radius: 3px; cursor: pointer;">
                                보기
                            </button>
                        </td>
                    </tr>
                `;
			}).join('');

			const finalHTML = `
                <div class="approval-section">
                    <h2>공지사항 목록</h2>
                    <div style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
                        <small style="color: #666;">총 ${data.length}개의 공지사항</small>
                        <button onclick="showUserNoticeForm()" 
                                style="padding: 8px 15px; background: #F56565; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer;">
                            새 공지사항 작성
                        </button>
                    </div>
                    <table class="notice-list-table">
                        <thead>
                            <tr>
                                <th style="width: 80px;">번호</th>
                                <th>제목</th>
                                <th style="width: 100px;">조회수</th>
                                <th style="width: 100px;">작업</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRows.length > 0 ? tableRows : '<tr><td colspan="4">등록된 공지사항이 없습니다.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            `;

			console.log("공지사항 목록 HTML 생성 완료");
			mainContentArea.innerHTML = finalHTML;
		})
		.catch(err => {
			console.error("공지사항 목록 조회 에러:", err);
			alert('공지사항 목록 조회 중 오류가 발생했습니다: ' + err.message);

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <h2>공지사항 목록</h2>
                    <div style="color: red; padding: 20px; border: 1px solid red; border-radius: 5px;">
                        공지사항 목록 로딩 실패: ${err.message}
                    </div>
                </div>
            `;
		});
}

// 공지사항 상세 보기 함수
function viewNoticeContent(b_id) {
	console.log("공지사항 상세 보기 - ID:", b_id);

	fetch('/notice/getNoticeList')
		.then(response => {
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			// 해당 ID의 공지사항 찾기
			const notice = data.find(item => item.b_id == b_id);

			if (!notice) {
				throw new Error('해당 공지사항을 찾을 수 없습니다.');
			}

			console.log('찾은 공지사항:', notice);

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <div style="margin-bottom: 20px;">
                        <button onclick="showNoticeList()"
                            style="padding: 8px 15px; background: #6c757d; color: white;
                            border: none; border-radius: 5px; cursor: pointer;">
                            ← 목록으로 돌아가기
                        </button>
                    </div>

                    <h2>공지사항 상세</h2>

                    <div class="form-group">
                        <h3>제목</h3>
                        <div id="title-content" class="editable-content" 
                             data-field="b_title" 
                             data-notice-id="${notice.b_id}"
                             style="padding: 10px; border: 1px solid #ddd; border-radius: 5px;
                             background-color: #f8f9fa; font-weight: bold; cursor: pointer;
                             transition: background-color 0.2s;">
                            ${notice.b_title || '제목 없음'}
                        </div>
                        <small style="color: #666;">더블클릭하여 수정</small>
                    </div>

                    <div class="form-group">
                        <h3>내용</h3>
                        <div id="content-content" class="editable-content" 
                             data-field="b_content" 
                             data-notice-id="${notice.b_id}"
                             style="padding: 8px; border: 1px solid #ddd; border-radius: 5px;
                             background-color: #f8f9fa; min-height: 200px; cursor: pointer;
                             transition: background-color 0.2s;">
                            ${notice.b_content || '내용이 없습니다.'}
                        </div>
                        <small style="color: #666;">더블클릭하여 수정</small>
                    </div>

                    <!-- 관리자 의견 섹션 -->
                    <div class="form-group" style="margin-top: 30px; padding-top: 20px; border-top: 2px solid #e9ecef;">
                        <h3 style="color: #007bff;">💬 관리자 의견</h3>
                        <textarea id="admin-comment" 
                                  placeholder="수정 사유나 의견을 입력해주세요..."
                                  style="width: 100%; min-height: 80px; padding: 10px; 
                                  border: 1px solid #ddd; border-radius: 5px; 
                                  font-family: inherit; font-size: 14px; resize: vertical; 
                                  box-sizing: border-box; background-color: #fff;">
                        </textarea>
                        <small style="color: #666;">
                            수정 중인 내용이 있으면 저장 버튼으로 한번에 저장됩니다.
                        </small>
                    </div>

                    <!-- 통합 저장 버튼 영역 (항상 표시) -->
                    <div id="save-button-area" style="margin-top: 20px; text-align: center;">
                        <div id="edit-status" style="padding: 15px; background: #e3f2fd; border: 1px solid #2196f3; 
                             border-radius: 8px; margin-bottom: 15px; display: none;">
                            <p style="margin: 0; color: #1976d2; font-weight: bold;">
                                📝 수정 중인 항목이 있습니다
                            </p>
                            <small style="color: #666;">수정사항을 저장하거나 취소할 수 있습니다.</small>
                        </div>
                        
                        <button id="unified-save-btn" 
                                style="padding: 12px 30px; background: #28a745; color: white;
                                border: none; border-radius: 6px; cursor: pointer; font-size: 16px;
                                font-weight: bold; margin-right: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.2);
                                opacity: 0.5;" disabled>
                            💾 변경사항 저장
                        </button>
                        
                        <button id="unified-cancel-btn" 
                                style="padding: 12px 30px; background: #6c757d; color: white;
                                border: none; border-radius: 6px; cursor: pointer; font-size: 16px;
                                font-weight: bold; box-shadow: 0 2px 5px rgba(0,0,0,0.2);
                                opacity: 0.5;" disabled>
                            ❌ 수정 취소
                        </button>
                    </div>

                    <div style="display: flex; justify-content: space-between; align-items: center;
                         padding: 10px; background-color: #e9ecef; border-radius: 5px; margin-top: 20px;">
                        <small style="color: #666;">
                            공지사항 번호: ${notice.b_id} | 조회수: ${notice.b_view || 0}
                        </small>
                    </div>

                    <div class="actions" style="margin-top: 20px;">
                        <button onclick="showNoticeList()"
                            style="padding: 8px; background: #007bff; color: white;
                            border: none; border-radius: 5px; cursor: pointer;">
                            목록으로
                        </button>
                    </div>
                </div>
            `;

			// 인라인 수정 이벤트 리스너 추가
			initializeInlineEdit();
			// 통합 버튼 이벤트 리스너 추가
			initializeUnifiedButtons();
		})
		.catch(err => {
			console.error('공지사항 상세 조회 에러:', err);
			alert('공지사항 상세 조회 실패: ' + err.message);
		});
}

// 전역 변수로 수정 상태 관리
let editingElements = new Map(); // field -> {element, inputElement, originalText}
let currentNoticeId = null;

// 인라인 수정 기능 초기화
function initializeInlineEdit() {
	const editableElements = document.querySelectorAll('.editable-content');

	editableElements.forEach(element => {
		// 호버 효과
		element.addEventListener('mouseenter', function() {
			if (!this.classList.contains('editing')) {
				this.style.backgroundColor = '#e9ecef';
			}
		});

		element.addEventListener('mouseleave', function() {
			if (!this.classList.contains('editing')) {
				this.style.backgroundColor = '#f8f9fa';
			}
		});

		// 더블클릭 이벤트
		element.addEventListener('dblclick', function() {
			startInlineEdit(this);
		});
	});
}

// 통합 버튼 이벤트 리스너
function initializeUnifiedButtons() {
	const saveBtn = document.getElementById('unified-save-btn');
	const cancelBtn = document.getElementById('unified-cancel-btn');

	saveBtn.addEventListener('click', saveAllChanges);
	cancelBtn.addEventListener('click', cancelAllChanges);
}

// 저장 버튼 영역 표시/숨김
function toggleSaveButtonArea() {
	const editStatus = document.getElementById('edit-status');
	const saveBtn = document.getElementById('unified-save-btn');
	const cancelBtn = document.getElementById('unified-cancel-btn');

	if (editingElements.size > 0) {
		// 수정 중일 때
		editStatus.style.display = 'block';
		saveBtn.disabled = false;
		cancelBtn.disabled = false;
		saveBtn.style.opacity = '1';
		cancelBtn.style.opacity = '1';
		saveBtn.style.cursor = 'pointer';
		cancelBtn.style.cursor = 'pointer';
	} else {
		// 수정 중이 아닐 때
		editStatus.style.display = 'none';
		saveBtn.disabled = true;
		cancelBtn.disabled = true;
		saveBtn.style.opacity = '0.5';
		cancelBtn.style.opacity = '0.5';
		saveBtn.style.cursor = 'not-allowed';
		cancelBtn.style.cursor = 'not-allowed';
	}
}

// 관리자 의견 가져오기 함수
function getAdminComment() {
	const adminCommentElement = document.getElementById('admin-comment');
	return adminCommentElement ? adminCommentElement.value.trim() : '';
}

// 인라인 수정 시작
function startInlineEdit(element) {
	if (element.classList.contains('editing')) return;

	element.classList.add('editing');
	const originalText = element.textContent;
	const field = element.getAttribute('data-field');
	const noticeId = element.getAttribute('data-notice-id');
	currentNoticeId = noticeId;

	// 기존 내용을 저장
	element.setAttribute('data-original', originalText);

	// 입력 필드 생성
	let inputElement;
	if (field === 'b_content') {
		inputElement = document.createElement('textarea');
		inputElement.style.minHeight = '200px';
		inputElement.style.resize = 'vertical';
	} else {
		inputElement = document.createElement('input');
		inputElement.type = 'text';
	}

	inputElement.value = originalText;
	inputElement.style.width = '100%';
	inputElement.style.padding = '8px';
	inputElement.style.border = '2px solid #007bff';
	inputElement.style.borderRadius = '5px';
	inputElement.style.fontFamily = 'inherit';
	inputElement.style.fontSize = 'inherit';
	inputElement.style.backgroundColor = 'white';
	inputElement.style.boxSizing = 'border-box';

	// 수정 중인 요소들을 맵에 저장
	editingElements.set(field, {
		element: element,
		inputElement: inputElement,
		originalText: originalText
	});

	// 기존 내용 숨기기
	element.style.display = 'none';

	// 입력 필드 추가
	element.parentNode.appendChild(inputElement);

	// 포커스 설정
	inputElement.focus();
	inputElement.select();

	// 저장 버튼 영역 활성화
	toggleSaveButtonArea();

	// Escape 키로 개별 취소
	inputElement.addEventListener('keydown', function(e) {
		if (e.key === 'Escape') {
			cancelSingleEdit(field);
		}
	});

	// Enter로 전체 저장 (제목인 경우)
	if (field === 'b_title') {
		inputElement.addEventListener('keypress', function(e) {
			if (e.key === 'Enter') {
				saveAllChanges();
			}
		});
	}

	// Ctrl+Enter로 전체 저장
	inputElement.addEventListener('keydown', function(e) {
		if (e.ctrlKey && e.key === 'Enter') {
			saveAllChanges();
		}
	});
}

// 개별 수정 취소
function cancelSingleEdit(field) {
	const editData = editingElements.get(field);
	if (!editData) return;

	const { element, inputElement } = editData;

	element.classList.remove('editing');
	element.style.display = 'block';
	element.style.backgroundColor = '#f8f9fa';

	inputElement.remove();
	editingElements.delete(field);

	toggleSaveButtonArea();
}

// 모든 변경사항 저장
function saveAllChanges() {
	if (editingElements.size === 0) {
		alert('수정된 내용이 없습니다.');
		return;
	}

	// 입력값 검증
	for (let [field, editData] of editingElements) {
		const newValue = editData.inputElement.value.trim();
		if (!newValue) {
			alert(`${field === 'b_title' ? '제목' : '내용'}을 입력해주세요.`);
			editData.inputElement.focus();
			return;
		}
	}

	// 로딩 상태로 변경
	const saveBtn = document.getElementById('unified-save-btn');
	const cancelBtn = document.getElementById('unified-cancel-btn');

	saveBtn.disabled = true;
	cancelBtn.disabled = true;
	saveBtn.textContent = '⏳ 저장 중...';
	saveBtn.style.backgroundColor = '#6c757d';

	// 모든 입력 필드 비활성화
	editingElements.forEach(editData => {
		editData.inputElement.disabled = true;
	});

	const adminComment = getAdminComment();
	const updates = [];

	// 각 수정사항을 배열에 저장
	editingElements.forEach((editData, field) => {
		const newValue = editData.inputElement.value.trim();
		updates.push({
			field: field,
			value: newValue,
			editData: editData
		});
	});

	// 순차적으로 업데이트 실행
	processUpdates(updates, adminComment, 0);
}

// 순차적으로 업데이트 처리
function processUpdates(updates, adminComment, index) {
	if (index >= updates.length) {
		// 모든 업데이트 완료
		handleAllUpdatesComplete();
		return;
	}

	const update = updates[index];

	updateNoticeField(currentNoticeId, update.field, update.value, adminComment)
		.then(success => {
			if (success) {
				// 성공 시 화면 업데이트
				update.editData.element.textContent = update.value;
				cleanupSingleEdit(update.field);

				// 다음 업데이트 처리
				processUpdates(updates, adminComment, index + 1);
			} else {
				throw new Error('서버에서 실패 응답');
			}
		})
		.catch(error => {
			console.error('수정 실패:', error);
			handleUpdateError(error.message);
		});
}

// 모든 업데이트 완료 처리
function handleAllUpdatesComplete() {
	// 관리자 의견 초기화
	const adminCommentElement = document.getElementById('admin-comment');
	if (adminCommentElement) {
		adminCommentElement.value = '';
	}

	// 저장 버튼 영역 비활성화
	toggleSaveButtonArea();

	// 성공 메시지
	showSuccessMessage('모든 변경사항이 저장되었습니다.');

	// 버튼 상태 복원
	resetButtonState();
}

// 업데이트 에러 처리
function handleUpdateError(errorMessage) {
	alert('수정 중 오류가 발생했습니다: ' + errorMessage);

	// 모든 입력 필드 다시 활성화
	editingElements.forEach(editData => {
		editData.inputElement.disabled = false;
	});

	// 버튼 상태 복원
	resetButtonState();
}

// 버튼 상태 복원
function resetButtonState() {
	const saveBtn = document.getElementById('unified-save-btn');
	const cancelBtn = document.getElementById('unified-cancel-btn');

	saveBtn.disabled = false;
	cancelBtn.disabled = false;
	saveBtn.textContent = '💾 변경사항 저장';
	saveBtn.style.backgroundColor = '#28a745';
}

// 단일 수정 정리
function cleanupSingleEdit(field) {
	const editData = editingElements.get(field);
	if (!editData) return;

	const { element, inputElement } = editData;

	element.classList.remove('editing');
	element.style.display = 'block';
	element.style.backgroundColor = '#f8f9fa';

	inputElement.remove();
	editingElements.delete(field);
}

// 모든 수정 취소
function cancelAllChanges() {
	if (editingElements.size === 0) return;

	if (confirm('모든 수정 내용을 취소하시겠습니까? 입력한 내용이 모두 사라집니다.')) {
		// 모든 수정사항 원복
		editingElements.forEach((editData, field) => {
			cleanupSingleEdit(field);
		});

		editingElements.clear();
		toggleSaveButtonArea();
	}
}

// 서버에 공지사항 필드 업데이트 요청
function updateNoticeField(noticeId, field, value, adminComment) {
	return fetch('/notice/updateField', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify({
			b_id: noticeId,
			field: field,
			value: value,
			admin_comment: adminComment || ''
		})
	})
		.then(response => {
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			return data.success === true;
		});
}

// 성공 메시지 표시
function showSuccessMessage(message) {
	const successDiv = document.createElement('div');
	successDiv.textContent = message;
	successDiv.style.position = 'fixed';
	successDiv.style.top = '20px';
	successDiv.style.right = '20px';
	successDiv.style.padding = '15px 25px';
	successDiv.style.backgroundColor = '#28a745';
	successDiv.style.color = 'white';
	successDiv.style.borderRadius = '8px';
	successDiv.style.zIndex = '9999';
	successDiv.style.boxShadow = '0 4px 15px rgba(0,0,0,0.2)';
	successDiv.style.fontWeight = 'bold';
	successDiv.style.animation = 'slideIn 0.3s ease-out';

	// 애니메이션 CSS 추가
	if (!document.getElementById('success-animation-style')) {
		const style = document.createElement('style');
		style.id = 'success-animation-style';
		style.textContent = `
            @keyframes slideIn {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
        `;
		document.head.appendChild(style);
	}

	document.body.appendChild(successDiv);

	setTimeout(() => {
		successDiv.style.animation = 'slideIn 0.3s ease-out reverse';
		setTimeout(() => successDiv.remove(), 300);
	}, 3000);
}

// --- View Render Functions ---

function showUserNoticeForm() {
	mainContentArea.innerHTML = `
            <div class="approval-section">
                <h2>신규 공지사항 작성</h2>
                <div class="form-group">
                    <input type="text" name="title" placeholder="제목 입력">
                </div>
                <div class="form-group">
                    <textarea name="content" placeholder="내용 입력"></textarea>
                </div>
                <div class="form-group">
                    <input type="text" name="admin_comment" placeholder="작성자 의견">
                </div>
                <div class="actions">
                    <button id="user-approval-btn">등록요청</button>
                </div>
            </div>
        `;
	document.getElementById('user-approval-btn').addEventListener('click', userApprovalFunction);
}

function showPendingNoticeList() {
	console.log("결재 대기 목록 조회 시작");

	fetch('/notice/getPendingList')
		.then(response => {
			console.log("서버 응답 상태:", response.status);
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			console.log("서버에서 받은 전체 데이터:", data);

			if (!Array.isArray(data)) {
				console.error("받은 데이터가 배열이 아닙니다:", typeof data);
				throw new Error("서버에서 잘못된 형식의 데이터를 받았습니다.");
			}

			// 결재대기와 수정요청 상태인 항목만 필터링
			const pendingOnlyNotices = data.filter(item => {
				const status = item.status ? item.status.toString().trim() : '';
				const isPending = status === '결재대기';
				const isModificationRequest = status === '수정요청';
				const shouldShow = isPending || isModificationRequest;

				console.log(`ID ${item.p_id}: 상태="${status}" -> 표시 여부: ${shouldShow} (결재대기: ${isPending}, 수정요청: ${isModificationRequest})`);
				return shouldShow;
			});

			console.log("필터링된 결재대기 항목:", pendingOnlyNotices);
			console.log("결재대기 항목 개수:", pendingOnlyNotices.length);

			let tableRows = pendingOnlyNotices.map(notice => {
				return `
                    <tr>
                        <td>${notice.p_id || '-'}</td>
                        <td>${notice.b_title || '-'}</td>
                        <td>${notice.b_created_at || '-'}</td>
                        <td>
                            <span class="status status-결재대기">결재대기</span>
                        </td>
                        <td>
                            <button onclick="viewNoticeDetail(${notice.p_id})">보기</button>
                        </td>
                    </tr>
                `;
			}).join('');

			const finalHTML = `
                <div class="approval-section">
                    <h2>결재 대기 목록</h2>
                    <div style="margin-bottom: 10px;">
                        <small>총 ${pendingOnlyNotices.length}개의 결재 대기 항목</small>
                    </div>
                    <table class="notice-list-table">
                        <thead>
                            <tr>
                                <th>결재 ID</th>
                                <th>제목</th>
                                <th>작성일</th>
                                <th>상태</th>
                                <th>작업</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRows.length > 0 ? tableRows : '<tr><td colspan="5">결재 대기중인 항목이 없습니다.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            `;

			console.log("최종 HTML 생성 완료");
			mainContentArea.innerHTML = finalHTML;
		})
		.catch(err => {
			console.error("에러 발생:", err);
			alert('데이터 조회 중 오류가 발생했습니다: ' + err.message);

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <h2>결재 대기 목록</h2>
                    <div style="color: red; padding: 20px; border: 1px solid red; border-radius: 5px;">
                        데이터 로딩 실패: ${err.message}
                    </div>
                </div>
            `;
		});
}

function viewNoticeDetail(p_id) {
	fetch('/notice/getPendingById?p_id=' + p_id)
		.then(response => {
			if (!response.ok) {
				throw new Error('Network response was not ok');
			}
			return response.json();
		})
		.then(notice => {
			console.log('받은 데이터:', notice);

			// 이미 처리된 문서인지 확인
			const isProcessed = notice.status === '승인' || notice.status === '반려';

			mainContentArea.innerHTML = `
                    <div class="approval-section">
                        <h2>공지사항 결재</h2>
                        <div class="status-bar">
                            <span class="status status-${notice.status}">${notice.status}</span>
                        </div>
                        <div class="form-group">
                            <h3>제목</h3>
                            <input type="text" value="${notice.b_title || ''}" readonly>
                        </div>
                        <div class="form-group">
                            <h3>내용</h3>
                            <textarea readonly>${notice.b_content || ''}</textarea>
                        </div>
                        <div class="form-group">
                            <h3>작성자 의견</h3>
                            <input type="text" value="${notice.admin_comment || ''}" readonly>
                        </div>
                        ${notice.rejected_comment ? `
                            <div class="form-group">
                                <h3>반려 사유</h3>
                                <input type="text" value="${notice.rejected_comment}" readonly>
                            </div>
                        ` : ''}
                        <hr>
                        ${!isProcessed ? `
                            <div class="comment-section">
                                <h3>반려 사유</h3>
                                <textarea id="rejection-reason" placeholder="반려 시 사유를 입력합니다."></textarea>
                            </div>
                            <div class="actions">
                                <button id="approve-btn" data-pid="${p_id}">승인</button>
                                <button id="reject-btn" data-pid="${p_id}">반려</button>
                            </div>
                        ` : `
                            <div class="actions">
                                <p>이미 처리된 문서입니다.</p>
                            </div>
                        `}
                    </div>
                `;

			// 처리되지 않은 문서에만 버튼 이벤트 등록
			if (!isProcessed) {
				document.getElementById('approve-btn').addEventListener('click', (e) => {
					if (confirm('승인하시겠습니까?')) {
						handleDecision(e.target.dataset.pid, '승인');
					}
				});

				document.getElementById('reject-btn').addEventListener('click', (e) => {
					const reason = document.getElementById('rejection-reason').value;
					if (!reason.trim()) {
						alert('반려 사유를 입력해야 합니다.');
						return;
					}
					if (confirm('반려하시겠습니까?')) {
						handleDecision(e.target.dataset.pid, '반려', reason);
					}
				});
			}
		})
		.catch(err => {
			console.error('Error:', err);
			alert('데이터 조회 실패: ' + err.message);
		});
}

// 처리된 문서 목록 (승인, 반려만 표시)
function viewCompleteList() {
	console.log("처리된 문서 목록 조회 시작");

	fetch('/notice/getPendingList')
		.then(response => {
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			console.log("처리된 문서 조회 - 원본 데이터:", data);

			// 승인 또는 반려된 문서만 필터링 (결재대기 제외)
			const completedNotices = data.filter(item => {
				const status = item.status ? item.status.toString().trim() : '';
				const isCompleted = status === '승인' || status === '반려';
				console.log(`ID ${item.p_id}: 상태="${status}" -> 처리완료 여부: ${isCompleted}`);
				return isCompleted;
			});

			console.log("필터링된 처리 완료 문서:", completedNotices);

			let tableRows = completedNotices.map(notice => {
				const status = notice.status ? notice.status.toString().trim() : '상태없음';
				return `
                    <tr>
                        <td>${notice.p_id || '-'}</td>
                        <td>${notice.b_title || '-'}</td>
                        <td>${notice.b_created_at || '-'}</td>
                        <td>
                            <span class="status status-${status}">${status}</span>
                        </td>
                        <td>${notice.rejected_comment || '-'}</td>
                        <td>
                            <button onclick="viewNoticeDetail(${notice.p_id})">상세 보기</button>
                        </td>
                    </tr>
                `;
			}).join('');

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <h2>처리된 문서 목록</h2>
                    <div style="margin-bottom: 10px;">
                        <small>총 ${completedNotices.length}개의 처리된 문서 (승인/반려)</small>
                    </div>
                    <table class="notice-list-table">
                        <thead>
                            <tr>
                                <th>결재 ID</th>
                                <th>제목</th>
                                <th>작성일</th>
                                <th>최종 상태</th>
                                <th>반려 사유</th>
                                <th>작업</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRows.length ? tableRows : '<tr><td colspan="6">처리된 문서가 없습니다.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            `;
		})
		.catch(err => {
			console.error("처리된 문서 조회 에러:", err);
			alert('처리된 문서 조회 중 오류가 발생했습니다: ' + err.message);
		});
}

//공지사항 전체 조회(처리된 문서)
function showAllNoticeList() {
	console.log("전체 목록 조회 시작");

	fetch('/notice/getPendingList')
		.then(response => {
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			console.log("전체 데이터:", data);

			// 상태별 개수 계산
			const statusCounts = data.reduce((counts, item) => {
				const status = item.status ? item.status.toString().trim() : '상태없음';
				counts[status] = (counts[status] || 0) + 1;
				return counts;
			}, {});

			console.log("상태별 개수:", statusCounts);

			let tableRows = data.map(notice => {
				const status = notice.status ? notice.status.toString().trim() : '상태없음';
				return `
                    <tr>
                        <td>${notice.p_id || '-'}</td>
                        <td>${notice.b_title || '-'}</td>
                        <td>${notice.b_created_at || '-'}</td>
                        <td>
                            <span class="status status-${status}">${status}</span>
                        </td>
                        <td>${notice.rejected_comment || '-'}</td>
                        <td>
                            <button onclick="viewNoticeDetail(${notice.p_id})">보기</button>
                        </td>
                    </tr>
                `;
			}).join('');

			const statusSummary = Object.entries(statusCounts)
				.map(([status, count]) => `${status}: ${count}개`)
				.join(', ');

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <h2>전체 문서 목록</h2>
                    <div style="margin-bottom: 10px;">
                        <small>총 ${data.length}개 항목 (${statusSummary})</small>
                    </div>
                    <table class="notice-list-table">
                        <thead>
                            <tr>
                                <th>결재 ID</th>
                                <th>제목</th>
                                <th>작성일</th>
                                <th>상태</th>
                                <th>반려 사유</th>
                                <th>작업</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRows.length ? tableRows : '<tr><td colspan="6">데이터가 없습니다.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            `;
		})
		.catch(err => {
			console.error("전체 목록 조회 에러:", err);
			alert('데이터 조회 중 오류가 발생했습니다: ' + err.message);
		});
}

//미구현된 페이지에 접속 시 디폴트 값
function showPlaceholderContent(menuKey) {
	const menuName = topMenu.querySelector(`[data-menu="${menuKey}"]`).textContent;
	mainContentArea.innerHTML = `<h1>${menuName}</h1><p>이 페이지의 콘텐츠는 아직 구현되지 않았습니다.</p>`;
}

//상위 메뉴에 따라 사이드 바 메뉴 업데이트
function updateSidebar(menuKey) {
	const menuName = topMenu.querySelector(`[data-menu="${menuKey}"]`).textContent;
	document.getElementById('sidebar-title').textContent = menuName;
	sidebarMenu.innerHTML = '';
	(submenus[menuKey] || []).forEach(item => {
		const a = document.createElement('a');
		a.href = '#';
		a.dataset.action = item;
		a.textContent = item;
		sidebarMenu.appendChild(document.createElement('li')).appendChild(a);
	});
}

//공지사항 등록 및 결재상신 Function
function userApprovalFunction() {
	const title = document.querySelector('input[name=title]').value;
	const content = document.querySelector('textarea[name=content]').value;
	const admin_comment = document.querySelector('input[name=admin_comment]').value;

	// 입력값 검증
	if (!title.trim()) {
		alert('제목을 입력해주세요.');
		return;
	}
	if (!content.trim()) {
		alert('내용을 입력해주세요.');
		return;
	}

	const createdAt = new Date().toISOString();
	const noticeData = {
		b_title: title,
		b_content: content,
		b_created_at: createdAt,
		status: '결재대기',
		admin_comment: admin_comment,
	};

	console.log("서버로 전송할 데이터:", noticeData);

	fetch('/notice/writeApproval', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(noticeData)
	})
		.then(response => {
			if (!response.ok) {
				throw new Error('사용자 권한 부적합!!');
			}
			return response.text();
		})
		.then(data => {
			console.log('서버 응답:', data);
			if (data === '데이터저장 성공') {
				alert('결재 상신 완료!');
				showPendingNoticeList();
			} else {
				alert('데이터 저장 실패: ' + data);
			}
		})
		.catch(err => {
			console.error('Error:', err);
			alert('Error: ' + err.message);
		});
}

// 결재 처리 함수 (승인/반려)
async function handleDecision(p_id, status, rejected_comment = '') {
	console.log(`서버로 결재 결정 전송: ID=${p_id}, 결정=${status}, 사유=${rejected_comment}`);

	const noticeData = {
		p_id: parseInt(p_id),
		status: status,
		rejected_comment: rejected_comment
	};

	try {
		const response = await fetch('/notice/uploadApprove', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(noticeData)
		});

		if (!response.ok) {
			throw new Error('결재권한 확인 필요!!');
		}

		const result = await response.text();
		console.log('서버 응답:', result);
		alert(result);

		// 결재 대기 목록 새로고침
		showPendingNoticeList();

	} catch (err) {
		console.error('Error:', err);
		alert('Error: ' + err.message);
	}
}

// 메인메뉴 버튼을 누르면 활성화 되도록 만들어주는 함수
topMenu.addEventListener('click', e => {
	if (e.target.tagName !== 'A') return;
	e.preventDefault();
	const menuKey = e.target.dataset.menu;
	updateSidebar(menuKey);
	if (menuKey === 'notice') showNoticeList();
	else if (menuKey === 'approval') showPendingNoticeList();
	else if (menuKey === 'management') showUserList();
	else if (menuKey === 'logout') logout();
	else if(menuKey === 'product') showCommodityList();
	else showPlaceholderContent(menuKey);
});

// 사이드바 버튼을 누르면 활성화 되도록 만들어주는 함수
sidebarMenu.addEventListener('click', e => {
	if (e.target.tagName !== 'A') return;
	e.preventDefault();
	const action = e.target.dataset.action;
	if (action === '결재 대기 목록') showPendingNoticeList();
	else if (action === '공지사항 등록') showUserNoticeForm();
	else if (action === '결재 문서 조회') viewCompleteList();
	else if (action === '공지사항 목록') showNoticeList();
	else if (action === '사용자 목록') showUserList();
	else if (action === '사용자 등록') showUserRegistrationForm();
	else if (action === '사용자 권한 변경') showUserRoleManagement();
	else if(action === '상품 목록') showCommodityList();
	else showPlaceholderContent(action);
});

document.addEventListener('DOMContentLoaded', () => {
	topMenu.querySelector('[data-menu="notice"]').click();
});

//----------------------------------------------------------------
//------------------사용자 관리 영역----------------------------------

// 사용자 목록 조회
function showUserList() {
	console.log("사용자 목록 조회 시작");

	fetch('/member/getUserList')
		.then(response => {
			console.log("사용자 목록 응답 상태:", response.status);
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			console.log("사용자 목록 데이터:", data);

			if (!Array.isArray(data)) {
				console.error("받은 데이터가 배열이 아닙니다:", typeof data);
				throw new Error("서버에서 잘못된 형식의 데이터를 받았습니다.");
			}

			console.log("사용자 개수:", data.length);

			let tableRows = data.map(user => {
				// 역할 클래스 결정 (user, master, admin)
				const roleClass = user.roll === 'admin' ? 'status-admin' :
					user.roll === 'master' ? 'status-master' : 'status-user';

				return `
                    <tr>
                        <td>${user.userid || '-'}</td>
                        <td>${user.username || '-'}</td>
                        <td>${user.name || '-'}</td>
                        <td>${user.email || '-'}</td>
                        <td>${user.phone || '-'}</td>
                        <td>
                            <span class="status ${roleClass}">${user.roll || '역할없음'}</span>
                        </td>
                        <td>
                            <button onclick="viewUserDetail(${user.userid})" 
                                    style="padding: 5px 10px; border: 1px solid #007bff; 
                                           background: #007bff; color: white; border-radius: 3px; cursor: pointer; margin-right: 5px;">
                                상세
                            </button>
                            <button onclick="editUser(${user.userid})" 
                                    style="padding: 5px 10px; border: 1px solid #28a745; 
                                           background: #28a745; color: white; border-radius: 3px; cursor: pointer;">
                                수정
                            </button>
                        </td>
                    </tr>
                `;
			}).join('');

			const finalHTML = `
                <div class="approval-section">
                    <h2>사용자 목록</h2>
                    <div style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
                        <small style="color: #666;">총 ${data.length}명의 사용자</small>
                        <button onclick="showUserRegistrationForm()" 
                                style="padding: 8px 15px; background: #28a745; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer;">
                            새 사용자 등록
                        </button>
                    </div>
                    
                    <style>
                        .status-user { 
                            background-color: #e3f2fd; 
                            color: #1976d2; 
                            padding: 4px 8px; 
                            border-radius: 4px; 
                            font-size: 12px; 
                            font-weight: bold;
                        }
                        .status-master { 
                            background-color: #fff3e0; 
                            color: #f57c00; 
                            padding: 4px 8px; 
                            border-radius: 4px; 
                            font-size: 12px; 
                            font-weight: bold;
                        }
                        .status-admin { 
                            background-color: #ffebee; 
                            color: #d32f2f; 
                            padding: 4px 8px; 
                            border-radius: 4px; 
                            font-size: 12px; 
                            font-weight: bold;
                        }
                    </style>
                    
                    <table class="notice-list-table">
                        <thead>
                            <tr>
                                <th style="width: 80px;">회원ID</th>
                                <th>유저 아이디</th>
                                <th>이름</th>
                                <th>이메일</th>
                                <th>전화번호</th>
                                <th style="width: 100px;">역할</th>
                                <th style="width: 150px;">작업</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRows.length > 0 ? tableRows : '<tr><td colspan="7">등록된 사용자가 없습니다.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            `;

			console.log("사용자 목록 HTML 생성 완료");
			mainContentArea.innerHTML = finalHTML;
		})
		.catch(err => {
			console.error("사용자 목록 조회 에러:", err);
			alert('사용자 목록 조회 중 오류가 발생했습니다: ' + err.message);

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <h2>사용자 목록</h2>
                    <div style="color: red; padding: 20px; border: 1px solid red; border-radius: 5px;">
                        사용자 목록 로딩 실패: ${err.message}
                    </div>
                </div>
            `;
		});
}

// 사용자 상세 정보 조회
function viewUserDetail(userid) {
	console.log("사용자 상세 정보 조회 - ID:", userid);

	fetch(`/member/getUserDetail?userid=${userid}`)
		.then(response => {
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(user => {
			console.log('사용자 상세 정보:', user);

			const roleClass = user.roll === 'admin' ? 'status-admin' :
				user.roll === 'master' ? 'status-master' : 'status-user';

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <div style="margin-bottom: 20px;">
                        <button onclick="showUserList()" 
                                style="padding: 8px 15px; background: #6c757d; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer;">
                            ← 목록으로 돌아가기
                        </button>
                    </div>
                    
                    <h2>사용자 상세 정보</h2>
                    
                    <style>
                        .status-user { background-color: #e3f2fd; color: #1976d2; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
                        .status-master { background-color: #fff3e0; color: #f57c00; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
                        .status-admin { background-color: #ffebee; color: #d32f2f; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
                    </style>
                    
                    <div class="form-group">
                        <h3>회원 ID</h3>
                        <div style="padding: 10px; border: 1px solid #ddd; border-radius: 5px; 
                                    background-color: #f8f9fa;">
                            ${user.userid || '-'}
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <h3>유저 아이디</h3>
                        <div style="padding: 10px; border: 1px solid #ddd; border-radius: 5px; 
                                    background-color: #f8f9fa;">
                            ${user.username || '-'}
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <h3>이름</h3>
                        <div style="padding: 10px; border: 1px solid #ddd; border-radius: 5px; 
                                    background-color: #f8f9fa;">
                            ${user.name || '-'}
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <h3>이메일</h3>
                        <div style="padding: 10px; border: 1px solid #ddd; border-radius: 5px; 
                                    background-color: #f8f9fa;">
                            ${user.email || '-'}
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <h3>전화번호</h3>
                        <div style="padding: 10px; border: 1px solid #ddd; border-radius: 5px; 
                                    background-color: #f8f9fa;">
                            ${user.phone || '-'}
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <h3>역할</h3>
                        <div style="padding: 10px; border: 1px solid #ddd; border-radius: 5px; 
                                    background-color: #f8f9fa;">
                            <span class="status ${roleClass}">${user.roll || '역할없음'}</span>
                        </div>
                    </div>
                    
                    <div class="actions" style="margin-top: 20px;">
                        <button onclick="editUser(${user.userid})" 
                                style="padding: 10px 20px; background: #28a745; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer; margin-right: 10px;">
                            수정
                        </button>
                        <button onclick="showUserList()" 
                                style="padding: 10px 20px; background: #007bff; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer;">
                            목록으로
                        </button>
                    </div>
                </div>
            `;
		})
		.catch(err => {
			console.error('사용자 상세 조회 에러:', err);
			alert('사용자 상세 조회 실패: ' + err.message);
		});
}

// 사용자 등록 폼
function showUserRegistrationForm() {
	mainContentArea.innerHTML = `
	        <div class="user-registration-container">
	            <div class="registration-header">
	                <div class="header-icon">
	                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
	                        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
	                        <circle cx="9" cy="7" r="4"></circle>
	                        <path d="M22 21v-2a4 4 0 0 0-3-3.87"></path>
	                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
	                    </svg>
	                </div>
	                <h2>새 사용자 등록</h2>
	                <p class="header-subtitle">시스템에 새로운 사용자를 추가합니다</p>
	            </div>

	            <form class="registration-form" id="userRegistrationForm">
	                <div class="form-row">
	                    <div class="form-field">
	                        <label for="username">
	                            <span class="field-icon">👤</span>
	                            유저 아이디 <span class="required">*</span>
	                        </label>
	                        <input type="text" id="username" name="username" 
	                               placeholder="로그인에 사용할 아이디를 입력하세요" 
	                               required>
	                        <div class="field-hint">영문, 숫자만 사용 가능 (4-20자)</div>
	                    </div>
	                </div>

	                <div class="form-row">
	                    <div class="form-field">
	                        <label for="password">
	                            <span class="field-icon">🔒</span>
	                            비밀번호 <span class="required">*</span>
	                        </label>
	                        <input type="password" id="password" name="password" 
	                               placeholder="안전한 비밀번호를 입력하세요" 
	                               required>
	                        <div class="field-hint">최소 8자 이상, 영문/숫자/특수문자 조합</div>
	                    </div>
	                </div>

	                <div class="form-row double">
	                    <div class="form-field">
	                        <label for="name">
	                            <span class="field-icon">📝</span>
	                            이름 <span class="required">*</span>
	                        </label>
	                        <input type="text" id="name" name="name" 
	                               placeholder="실명을 입력하세요" 
	                               required>
	                    </div>
	                    <div class="form-field">
	                        <label for="roll">
	                            <span class="field-icon">⚡</span>
	                            역할 <span class="required">*</span>
	                        </label>
	                        <select id="roll" name="roll" required>
	                            <option value="" disabled selected>역할을 선택하세요</option>
	                            <option value="USER">👤 사용자</option>
	                            <option value="MASTER">⭐ 마스터</option>
	                            <option value="ADMIN">👑 관리자</option>
	                        </select>
	                    </div>
	                </div>

	                <div class="form-row double">
	                    <div class="form-field">
	                        <label for="email">
	                            <span class="field-icon">📧</span>
	                            이메일 <span class="required">*</span>
	                        </label>
	                        <input type="email" id="email" name="email" 
	                               placeholder="example@company.com" 
	                               required>
	                    </div>
	                    <div class="form-field">
	                        <label for="phone">
	                            <span class="field-icon">📱</span>
	                            전화번호
	                        </label>
	                        <input type="tel" id="phone" name="phone" 
	                               placeholder="010-1234-5678" 
	                               maxlength="13"
	                               pattern="[0-9]{3}-[0-9]{4}-[0-9]{4}">
	                    </div>
	                </div>

	                <div class="form-actions">
	                    <button type="button" class="btn btn-secondary" onclick="showUserList()">
	                        <span class="btn-icon">↩️</span>
	                        취소
	                    </button>
	                    <button type="submit" class="btn btn-primary" onclick="registerUser(); return false;">
	                        <span class="btn-icon">✅</span>
	                        사용자 등록
	                    </button>
	                </div>
	            </form>
	        </div>
    `;

	// 전화번호 자동 포맷팅
	document.getElementById('phone').addEventListener('input', function(e) {
		let value = e.target.value.replace(/[^0-9]/g, '');
		if (value.length >= 3 && value.length <= 6) {
			value = value.slice(0, 3) + '-' + value.slice(3);
		} else if (value.length >= 7) {
			value = value.slice(0, 3) + '-' + value.slice(3, 7) + '-' + value.slice(7, 11);
		}
		e.target.value = value;
	});
}

// 사용자 등록 처리
function registerUser() {
	const username = document.getElementById('username').value;
	const password = document.getElementById('password').value;
	const name = document.getElementById('name').value;
	const email = document.getElementById('email').value;
	const phone = document.getElementById('phone').value;
	const roll = document.getElementById('roll').value;

	// 입력값 검증
	if (!username.trim()) {
		alert('유저 아이디를 입력해주세요.');
		return;
	}
	if (!password.trim()) {
		alert('비밀번호를 입력해주세요.');
		return;
	}
	if (!name.trim()) {
		alert('이름을 입력해주세요.');
		return;
	}
	if (!email.trim()) {
		alert('이메일을 입력해주세요.');
		return;
	}
	if (!phone.trim()) {
		alert('전화번호를 입력해주세요.');
		return;
	}
	if (!roll) {
		alert('역할을 선택해주세요.');
		return;
	}

	// 역할 유효성 검사
	if (!['USER', 'MASTER', 'ADMIN'].includes(roll)) {
		alert('올바른 역할을 선택해주세요.');
		return;
	}

	// 이메일 형식 검증
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	if (!emailRegex.test(email)) {
		alert('올바른 이메일 형식을 입력해주세요.');
		return;
	}

	// 전화번호 형식 검증
	const phoneRegex = /^\d{3}-\d{4}-\d{4}$/;
	if (!phoneRegex.test(phone)) {
		alert('올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678)');
		return;
	}

	const userData = {
		username: username,
		password: password,
		name: name,
		email: email,
		phone: phone,
		roll: roll
	};

	console.log("서버로 전송할 사용자 데이터:", userData);

	fetch('/member/registMember', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(userData)
	})
		.then(response => {
			if (!response.ok) {
				throw new Error('사용자 등록 권한이 부족합니다.');
			}
			return response.text();
		})
		.then(data => {
			console.log('서버 응답:', data);
			if (data === '사용자 등록 성공') {
				alert('사용자 등록이 완료되었습니다.');
				showUserList();
			} else {
				alert('사용자 등록 실패: ' + data);
			}
		})
		.catch(err => {
			console.error('Error:', err);
			alert('Error: ' + err.message);
		});
}

// 사용자 수정 폼
function editUser(userid) {
	console.log("사용자 수정 폼 - ID:", userid);

	fetch(`/member/getUserDetail?userid=${userid}`)
		.then(response => {
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(user => {
			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <h2>사용자 정보 수정</h2>
                    <input type="hidden" id="edit-userid" value="${user.userid}">
                    
                    <div class="form-group">
                        <h3>회원 ID</h3>
                        <div style="padding: 10px; border: 1px solid #ddd; border-radius: 5px; 
                                    background-color: #f8f9fa; color: #666;">
                            ${user.userid} (수정 불가)
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <h3>유저 아이디</h3>
                        <input type="text" id="edit-username" value="${user.username || ''}" placeholder="유저 아이디 입력">
                    </div>
                    
                    <div class="form-group">
                        <h3>비밀번호</h3>
                        <input type="password" id="edit-password" placeholder="새 비밀번호 입력 (변경 시에만)">
                        <small style="color: #666;">비밀번호를 변경하지 않으려면 비워두세요.</small>
                    </div>
                    
                    <div class="form-group">
                        <h3>이름</h3>
                        <input type="text" id="edit-name" value="${user.name || ''}" placeholder="이름 입력">
                    </div>
                    
                    <div class="form-group">
                        <h3>이메일</h3>
                        <input type="email" id="edit-email" value="${user.email || ''}" placeholder="이메일 주소 입력">
                    </div>
                    
                    <div class="form-group">
                        <h3>전화번호</h3>
                        <input type="tel" id="edit-phone" value="${user.phone || ''}" placeholder="전화번호 입력" maxlength="13">
                    </div>                  
             
                    
                    <div class="actions">
                        <button onclick="updateUser()" 
                                style="padding: 10px 20px; background: #28a745; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer; margin-right: 10px;">
                            수정 완료
                        </button>
                        <button onclick="deleteUser(${user.userid})" 
                                style="padding: 10px 20px; background: #dc3545; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer; margin-right: 10px;">
                            사용자 삭제
                        </button>
                        <button onclick="showUserList()" 
                                style="padding: 10px 20px; background: #6c757d; color: white; 
                                       border: none; border-radius: 5px; cursor: pointer;">
                            취소
                        </button>
                    </div>
                </div>
            `;

			// 전화번호 자동 포맷팅
			document.getElementById('edit-phone').addEventListener('input', function(e) {
				let value = e.target.value.replace(/[^0-9]/g, '');
				if (value.length >= 3 && value.length <= 6) {
					value = value.slice(0, 3) + '-' + value.slice(3);
				} else if (value.length >= 7) {
					value = value.slice(0, 3) + '-' + value.slice(3, 7) + '-' + value.slice(7, 11);
				}
				e.target.value = value;
			});
		})
		.catch(err => {
			console.error('사용자 정보 로드 에러:', err);
			alert('사용자 정보 로드 실패: ' + err.message);
		});
}

// 사용자 정보 업데이트 처리
function updateUser() {
	const userid = document.getElementById('edit-userid').value;
	const username = document.getElementById('edit-username').value;
	const password = document.getElementById('edit-password').value;
	const name = document.getElementById('edit-name').value;
	const email = document.getElementById('edit-email').value;
	const phone = document.getElementById('edit-phone').value;
	const roll = document.getElementById('edit-roll').value;

	// 입력값 검증
	if (!username.trim()) {
		alert('유저 아이디를 입력해주세요.');
		return;
	}
	if (!name.trim()) {
		alert('이름을 입력해주세요.');
		return;
	}
	if (!email.trim()) {
		alert('이메일을 입력해주세요.');
		return;
	}
	if (!phone.trim()) {
		alert('전화번호를 입력해주세요.');
		return;
	}
	if (!roll) {
		alert('역할을 선택해주세요.');
		return;
	}

	// 역할 유효성 검사
	if (!['user', 'master', 'admin'].includes(roll)) {
		alert('올바른 역할을 선택해주세요.');
		return;
	}

	// 이메일 형식 검증
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	if (!emailRegex.test(email)) {
		alert('올바른 이메일 형식을 입력해주세요.');
		return;
	}

	// 전화번호 형식 검증
	const phoneRegex = /^\d{3}-\d{4}-\d{4}$/;
	if (!phoneRegex.test(phone)) {
		alert('올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678)');
		return;
	}

	const userData = {
		userid: parseInt(userid),
		username: username,
		name: name,
		email: email,
		phone: phone,
		roll: roll
	};

	// 비밀번호가 입력된 경우에만 포함
	if (password.trim()) {
		userData.password = password;
	}

	console.log("서버로 전송할 수정 데이터:", userData);

	fetch('/member/updateUser', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(userData)
	})
		.then(response => {
			if (!response.ok) {
				throw new Error('사용자 정보 수정 권한이 부족합니다.');
			}
			return response.text();
		})
		.then(data => {
			console.log('서버 응답:', data);
			if (data === '사용자 정보 수정 성공') {
				alert('사용자 정보가 수정되었습니다.');
				showUserList();
			} else {
				alert('사용자 정보 수정 실패: ' + data);
			}
		})
		.catch(err => {
			console.error('Error:', err);
			alert('Error: ' + err.message);
		});
}

// 사용자 삭제
function deleteUser(userid) {
	if (!confirm('정말로 이 사용자를 삭제하시겠습니까?\n삭제된 사용자 정보는 복구할 수 없습니다.')) {
		return;
	}

	fetch('/member/deleteUser', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify({ userid: userid })
	})
		.then(response => {
			if (!response.ok) {
				throw new Error('사용자 삭제 권한이 부족합니다.');
			}
			return response.text();
		})
		.then(data => {
			console.log('서버 응답:', data);
			if (data === '사용자 삭제 성공') {
				alert('사용자가 삭제되었습니다.');
				showUserList();
			} else {
				alert('사용자 삭제 실패: ' + data);
			}
		})
		.catch(err => {
			console.error('Error:', err);
			alert('Error: ' + err.message);
		});
}

// 사용자 권한 변경
function showUserRoleManagement() {
	console.log("사용자 권한 변경 페이지");

	fetch('/member/getUserList')
		.then(response => {
			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}
			return response.json();
		})
		.then(data => {
			let tableRows = data.map(user => {
				return `
                    <tr>
                        <td>${user.userid || '-'}</td>
                        <td>${user.username || '-'}</td>
                        <td>${user.name || '-'}</td>
                        <td>${user.email || '-'}</td>
                        <td>
                            <select id="role-${user.userid}">
                                <option value="USER" ${user.roll === 'USER' ? 'selected' : ''}>사용자</option>
                                <option value="MASTER" ${user.roll === 'MASTER' ? 'selected' : ''}>마스터</option>
                                <option value="ADMIN" ${user.roll === 'ADMIN' ? 'selected' : ''}>관리자</option>
                            </select>
                        </td>
                        <td>
                            <button onclick="applyRoleChange(${user.userid})" 
                                    style="padding: 5px 10px; background: #28a745; color: white; 
                                           border: none; border-radius: 3px; cursor: pointer;">
                                적용
                            </button>
                        </td>
                    </tr>
                `;
			}).join('');

			mainContentArea.innerHTML = `
                <div class="approval-section">
                    <h2>사용자 권한 변경</h2>
                    <div style="margin-bottom: 15px;">
                        <small style="color: #666;">사용자의 권한을 변경할 수 있습니다.</small>
                    </div>
                    <table class="notice-list-table">
                        <thead>
                            <tr>
                                <th>회원ID</th>
                                <th>유저 아이디</th>
                                <th>이름</th>
                                <th>이메일</th>
                                <th>역할</th>
                                <th>작업</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRows.length > 0 ? tableRows : '<tr><td colspan="6">사용자가 없습니다.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            `;
		})
		.catch(err => {
			console.error('사용자 권한 변경 페이지 로드 에러:', err);
			alert('사용자 권한 변경 페이지 로드 실패: ' + err.message);
		});
}

// 권한 변경 적용
function applyRoleChange(userid) {
	const newRole = document.getElementById(`role-${userid}`).value;

	if (confirm(`사용자 권한을 '${newRole}'로 변경하시겠습니까?`)) {
		const userData = {
			userid: userid,
			roll: newRole
		};

		fetch('/member/changeUserRoll', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(userData)
		})
			.then(response => {
				if (!response.ok) {
					throw new Error('권한 변경 권한이 부족합니다.');
				}
				return response.text();
			})
			.then(data => {
				console.log('권한 변경 결과:', data);
				if (data === '권한 변경 성공') {
					alert('사용자 권한이 변경되었습니다.');
					showUserRoleManagement(); // 페이지 새로고침
				} else {
					alert('권한 변경 실패: ' + data);
				}
			})
			.catch(err => {
				console.error('Error:', err);
				alert('Error: ' + err.message);
			});
	}
}

/**************상품 관련 함수****************/

function showCommodityList() {
	mainContentArea.innerHTML = `
	<div class="wrapper">
	    <!-- ---------- Header + Search ---------- -->
	    <header class="page-header">
	        <h1>퇴직연금 상품찾기</h1>
	        <div class="search-area">
	            <div class="search-input">
	                <span class="icon"><i class="fas fa-search"></i></span>
	                <input type="text" id="searchKeyword" placeholder="어떤 상품을 찾으세요?" />
	            </div>
	            <button class="search-btn" id="searchBtn">검색</button>
	            <button class="filter-btn" id="filterBtn">
				    <i class="fas fa-sliders-h"></i>
				</button>
	        </div>
	        
	        <div class="total-cnt">
	    	</div>
	    </header>
		
	    <!-- ---------- Tabs ---------- -->
	    <div class="choice">
		    <div id="buttons">
		        <button class="commodity-button active" id="fundBtn"><span>펀드</span></button>
		        <button class="commodity-button" id="etfBtn"><span>ETF</span></button>
		        <button class="commodity-button" id="tdfBtn"><span>TDF</span></button>
		        <button class="commodity-button" id="defaultBtn"><span>디폴트옵션</span></button>
		        <button class="commodity-button" id="guaranteeBtn"><span>예금</span></button>
		    </div>
		
		    <!-- ---------- Filters ---------- -->
		    <form class="selectors" onsubmit="return false;"> </form>
	    </div>


	    <!-- ---------- Results ---------- -->
	    <main id="commodity_main"></main>
	    
	    <!-- ---------- ANALYSIS MODAL ---------- -->
		<div id="analysisModal" class="modal">
		    <div class="modal-content">
		        <div class="modal-header">
		            <h2 id="modalTitle">상품 분석</h2>
		            <button class="close-btn" id="modalCloseBtn">&times;</button>
		        </div>
		        <select id="modalNavSelect" hidden="hidden"></select>
		        <nav class="modal-nav" id="modalNav">
		            <button data-key="overviewUrl" class="active">개요</button>
		            <button data-key="navUrl">기준가</button>
		            <button data-key="performanceChartUrl">성과 차트</button>
		            <button data-key="performanceAnalysisUrl">성과 분석</button>
		            <button data-key="riskAnalysisUrl">위험 분석</button>
		            <button data-key="portfolioAnalysisUrl">포트폴리오</button>
		            <button data-key="holdingsUrl">보유종목</button>
		        </nav>
				<div class="files" id="modalFiles">
				  <a id="file1Link" class="file-link" href="#" target="_blank">
				    <i class="fas fa-file-pdf"></i> 투자설명서
				  </a>
				  <a id="file2Link" class="file-link" href="#" target="_blank">
				    <i class="fas fa-file-pdf"></i> 상품약관
				  </a>
				  <a id="file3Link" class="file-link" href="#" target="_blank">
				    <i class="fas fa-file-pdf"></i> 간이투자설명서
				  </a>
				</div>
		        <div class="modal-body"><iframe id="analysisFrame" title="analysis"></iframe></div>
		    </div>
		</div>
		
		<!-- ▽ 기존 analysisModal 아래, </div> 바로 앞에 모바일 필터 모달 추가 -->
		<div id="filterModal" class="modal">                 <!-- 기존 .modal 재활용 -->
		    <div class="modal-content mobile-filter">
		        <div class="modal-header">
		            <h2 style="font-size:1rem;">검색 필터</h2>
		            <button class="close-btn" id="filterCloseBtn">&times;</button>
		        </div>
		
		        <div class="modal-body" id="filterBody"></div>
		
		        <button id="filterApplyBtn"
		                style="margin:1rem auto .8rem; padding:.6rem 2rem;"
		                class="search-btn">적용하기
		        </button>
		    </div>
		</div>
		<button id="loadMoreBtn">더보기</button>
	</div>`;
	commodityInit();
}


function commodityInit() {	
	/* dom 선택 helper */
	function qs(sel, scope = document){return scope.querySelector(sel);}
	const qsa     = (sel,scope=document)=>[...scope.querySelectorAll(sel)];
		
	// tab active helper
	function setActiveTab(id){
	    document.querySelectorAll('#buttons .commodity-button').forEach(btn=>btn.classList.remove('active'));
	    qs('#'+id).classList.add('active');
	}

	/* ajax helpers */
	const postJson = (url, body) => fetch(url, {
	    method: 'POST',
	    headers: { 'Content-Type': 'application/json' },
	    body: JSON.stringify(body)
	})
	.then(res => {
	    if (!res.ok) throw new Error(`요청 실패: ${res.status}`);
	    return res.json();
	})
	.then(json => {
		if(json.totalCnt != null) {
	        totalCount = json.totalCnt ?? 0;   // 전체 건수 갱신
	        
	        qs('.total-cnt').innerHTML = '';
	        const totalMsg = document.createElement('div');
	        totalMsg.classList.add('total-message');
	        totalMsg.innerHTML = `총 <span class="total-number"></span>개의 상품이 있습니다.`;
	        qs('.total-cnt').appendChild(totalMsg);
	        qs('.total-number').textContent = totalCount.toLocaleString();
	        
		}
		if(json.entityList != null) {
			return json.entityList ?? [];	
		}
		
		return json.analysisEntity;
	});

	/* render helpers */
	const clearMain = () => {
		const cards = commodityMain.querySelectorAll('.fund-card');
	    cards.forEach(card => card.remove());
	    const message = commodityMain.querySelector('#no-commodity-message');
	    if(message != null) message.remove();
	}
	

	/* === 모바일 필터 모달 === */
	const filterBtn       = qs('#filterBtn');
	const filterModal     = qs('#filterModal');
	const filterCloseBtn  = qs('#filterCloseBtn');
	const filterApplyBtn  = qs('#filterApplyBtn');
	const selectorsElm    = qs('.selectors');   // 데스크톱용 드롭박스
	const filterBody      = qs('#filterBody');  // 모달 안 자리

	/* 열기 – 모달 안에 selectors 옮기기 */
	filterBtn.addEventListener('click', ()=>{
	  selectorsElm.style.display = 'grid';
	  filterBody.appendChild(selectorsElm);          // ⬇ 모달 안으로 이동
	  filterModal.classList.add('open');
	});

	/* 닫기 – 다시 원래 자리로 */
	function closeFilterModal(){
	  selectorsElm.style.display = 'none';
	  qs('.choice').appendChild(selectorsElm);       // ⬆ 헤더로 복귀
	  filterModal.classList.remove('open');
	}
	filterCloseBtn.addEventListener('click', closeFilterModal);

	/* 적용하기 버튼 – 검색 실행 후 닫기 */
	filterApplyBtn.addEventListener('click', ()=>{
	  closeFilterModal();
	  handleSearch();   // 이미 정의된 함수 그대로 사용
	});
	/* state */
	let currentPage = 0; //현재 페이지
	const pageSize = 10; //페이지 크기
	let totalCount = 0; //불러온 상품 총 개수(실제 불러온 값이 아니라, 검색 대상 상품 기준 개수)
	let lastRequest  = { url:'', body:{} }; //가장 최근 조회 상태

	let currentTab = 'fund';
	const commodityMain = qs('#commodity_main');

	    
	const metricBox = (val,label) => {
		const display = isNaN(val)?val:Number(val).toFixed(2)+'%';
		if(label === '1개월' || label ==='누적') {
			return `<div class="metric-box omitable"><span class="value">${display}</span><span class="label">${label}</span></div>`;
		} else {
			return `<div class="metric-box"><span class="value">${display}</span><span class="label">${label}</span></div>`;	
		}
	};

	const formatDate = str => str?`${str.slice(0,4)}-${str.slice(4,6)}-${str.slice(6)}`:'';


	//펀드 카드
	function createFundCard(f){
	    return buildCard(`
	        <div class="fund-name">${f.prodName}</div>
	        <div class="fund-sub">${f.risk} | <span>${f.fundTypeCd}</span> | ${f.manager}</div>
	        <div class="fund-sub" style="font-size:.75rem;color:#999;">
	            <span style="margin-right: 3px;">• 기준가: ${parseFloat(f.nav).toLocaleString()}</span>
	            <span style="margin-right: 3px;">• 설정일: ${formatDate(f.setDate)}</span>
	            <span>• 총 보수: ${f.totalFee}%</span>
	        </div>
	        <div class="metric-grid">
	            ${metricBox(f.oneMonth,'1개월')}
	            ${metricBox(f.threeMonth,'3개월')}
	            ${metricBox(f.sixMonth,'6개월')}
	            ${metricBox(f.year,'12개월')}
	            ${metricBox(f.accum,'누적')}
	        </div>`, f.prodId);
	}

	//디폴트 카드
	function createDefaultCard(d){
		const extractFileName = url => url.substring(url.lastIndexOf('/') + 1);

		const guide = extractFileName(d["guideUrl"]);
		const desc = extractFileName(d["descUrl"]);
		
		const ecguide = encodeURIComponent(guide);
		const ecdesc = encodeURIComponent(desc);
		
		const commonPart = d["guideUrl"].substring(0, d["guideUrl"].lastIndexOf('/')+1);
		const encodedguide = commonPart + ecguide;
		const encodedDesc = commonPart + ecdesc;
		
	    const riskLabel = d.risk ? `<span style="color:#D71921;">${d.risk}</span>` : '';
	    const gradeLabel = d.riskGrade ? ` (등급 ${d.riskGrade})` : '';
		
	    return buildCard(`
	        <div class="notFund-name">${d.prodName}</div>
	        ${d.risk || d.riskGrade ? `<div class="fund-sub">위험도: ${riskLabel}${gradeLabel}</div>` : ''}
	        <ul style="margin-top:.8rem;font-size:.85rem;color:#555;padding-left:1rem;list-style:disc;">
	            ${d.subProd1?`<li>${d.subProd1}</li>`:''}
	            ${d.subProd2?`<li>${d.subProd2}</li>`:''}
	        </ul>
			<div class="nofundfiles">
			  <a id="file1Link" class="file-link" href="${encodedDesc}" target="_blank">
			    <i class="fas fa-file-pdf"></i>상품설명서
			  </a>
			  <a id="file2Link" class="file-link" href="${encodedguide}" target="_blank">
			    <i class="fas fa-file-pdf"></i>상품안내
			  </a>
			</div>`, null);
	}

	//원리금 보장 카드
	function createGuaranteeCard(g){
		
		const extractFileName = url => url.substring(url.lastIndexOf('/') + 1);

		const terms = extractFileName(g["termsUrl"]);
		const desc = extractFileName(g["descUrl"]);
		const threeMonth = extractFileName(g["threeMonth"]);
		
		const ecTerms = encodeURIComponent(terms);
		const ecdesc = encodeURIComponent(desc);
		const ecthreeMonth = encodeURIComponent(threeMonth);
		
		const commonPart = g["termsUrl"].substring(0, g["termsUrl"].lastIndexOf('/')+1);
		const encodedTerms = commonPart + ecTerms;
		const encodedDesc = commonPart + ecdesc;
		const encodedThreeMonth= commonPart + ecthreeMonth;
		
	    return buildCard(`
	        <div class="notFund-name">${g.bank} ${g.prodName} (${g.maturityDate})</div>
	        <div class="metric-grid" style="grid-template-columns:repeat(3,1fr);">
	            ${metricBox(g.dbYn,'DB')}
	            ${metricBox(g.dcYn,'DC')}
	            ${metricBox(g.irpYn,'IRP')}
	        </div>
			<div class="nofundfiles">
			  <a id="file1Link" class="file-link" href="${encodedTerms}" target="_blank">
			    <i class="fas fa-file-pdf"></i> 약관
			  </a>
			  <a id="file2Link" class="file-link" href="${encodedDesc}" target="_blank">
			    <i class="fas fa-file-pdf"></i> 설명서
			  </a>
			  <a id="file3Link" class="file-link" href="${encodedThreeMonth}" target="_blank">
			    <i class="fas fa-file-pdf"></i> 3개월추이
			  </a>
			</div>`, null);
	}

	// 카드 생성시 스타일 fund-card로 지정하고 상품 id 값 저장
	function buildCard(innerHtml, prodId){
	    const div=document.createElement('div');
	    div.className='fund-card';
	    if(prodId) div.dataset.prodid = prodId;
	    div.innerHTML = innerHtml;
	    return div;
	}

	//카테고리별로 카드 구성 각각 생성
	function renderJson(data){
	    const arr = Array.isArray(data)?data:[data];
	    console.log(arr);
	    // 상품이 없는 경우
	    if (arr.length === 0) {
	        commodityMain.innerHTML = `
	        	<div id="no-commodity-message">
	        		<div style="display: inline-block;">조회된 상품이 없습니다.</div>
	           </div>
	            `; 
	    } else {
	        arr.forEach(item=>{
	            let card;
	            switch(currentTab){
	                case 'default':   card=createDefaultCard(item); break;
	                case 'guarantee': card=createGuaranteeCard(item); break;
	                default: card = createFundCard(item); break;
	            }
	            commodityMain.appendChild(card);
	        });        	
	    }
	    
	    // 목록 다 그리고 버튼 보일지 말지 정함.
	    const loadMoreBtn = qs('#loadMoreBtn');
	    toggleLoadMoreButton(loadMoreBtn);
	}

	// 목록 보일지 말지 정하는 함수
	function toggleLoadMoreButton(loadMoreBtn) {
	  const totalPages = Math.ceil(totalCount / pageSize);
	  loadMoreBtn.style.display = currentPage < totalPages - 1 ? 'block' : 'none';
	}

	// 카테고리 변경 시에 실행되는 함수(fund, etf, tdf인 경우에)
	// 카테고리 변경 시에는 폼 초기화 및 폼 변경
	const fetchAndRender = (url, body) => {
	    // 카테고리를 옮기면 항상 새로운 결과 목록 --> 더보기 버튼은 불러온 목록을 기준으로 더 보여줘야 함.
	   	currentPage = 0;
	   	lastRequest = {url, body}; // 카테고리 옮겼을 때의 상태
	   
		const form = qs(".selectors");
		const searchBar = qs('.search-area');
		const keywordDom = qs('#searchKeyword');
		keywordDom.value = '';
		switch(url) {
			case '/commodity/fund':
				searchBar.classList.add('visible');
				form.innerHTML = 
					`<!-- 위험등급 -->
		    	    <div class="dropdown-box" id="riskBox">
		                <button type="button" class="dropdown-toggle">위험등급 선택 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="checkbox" name="riskGrade" value="1"/> 매우높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="2"/> 높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="3"/> 다소높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="4"/> 보통위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="5"/> 낮은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="6"/> 매우낮은위험</label>
		                </div>
		            </div>
		
		            <!-- 펀드유형 -->
		            <div class="dropdown-box" id="typeBox">
		                <button type="button" class="dropdown-toggle">펀드 유형 선택 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="checkbox" name="type" value="MMF"/> MMF</label>
		                    <label><input type="checkbox" name="type" value="채권형"/> 채권형</label>
		                    <label><input type="checkbox" name="type" value="채권혼합형"/> 채권혼합형</label>
		                    <label><input type="checkbox" name="type" value="주식혼합형"/> 주식혼합형</label>
		                    <label><input type="checkbox" name="type" value="주식형"/> 주식형</label>
		                    <label><input type="checkbox" name="type" value="파생상품형"/> 파생상품형</label>
		                    <label><input type="checkbox" name="type" value="재간접"/> 재간접</label>
		                </div>
		            </div>
		
		            <!-- 채널구분 -->
		            <div class="dropdown-box" id="channelBox">
		                <button type="button" class="dropdown-toggle">채널 구분 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="radio" name="channel" value="전체" checked/> 전체</label>
		                    <label><input type="radio" name="channel" value="온라인전용"/> 온라인전용</label>
		                </div>
		            </div>
		            
		            <!-- 수익률 정렬 -->
		    	    <div class="dropdown-box" id="rateBox">
		                <button type="button" class="dropdown-toggle">수익률 정렬 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="radio" name="interPeriod" value="1"/> 1개월</label>
		                    <label><input type="radio" name="interPeriod" value="3"/> 3개월</label>
		                    <label><input type="radio" name="interPeriod" value="6"/> 6개월</label>
		                    <label><input type="radio" name="interPeriod" value="12"/> 12개월</label>
		                    <label><input type="radio" name="interPeriod" value="100"/> 누적</label>
		                </div>
	            	</div>`;  break;
			case '/commodity/etf':
				searchBar.classList.add('visible');
				form.innerHTML = 
					`<!-- 위험등급 -->
		    	    <div class="dropdown-box" id="riskBox">
		                <button type="button" class="dropdown-toggle">위험등급 선택 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="checkbox" name="riskGrade" value="1"/> 매우높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="2"/> 높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="3"/> 다소높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="4"/> 보통위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="5"/> 낮은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="6"/> 매우낮은위험</label>
		                </div>
		            </div>
		
		            <!-- 펀드유형 -->
		            <div class="dropdown-box" id="typeBox">
		                <button type="button" class="dropdown-toggle">펀드 유형 선택 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="checkbox" name="type" value="주식형"/> 주식형</label>
		                    <label><input type="checkbox" name="type" value="채권형"/> 채권형</label>
		                    <label><input type="checkbox" name="type" value="혼합형"/> 혼합형</label>
		                    <label><input type="checkbox" name="type" value="원자재"/> 원자재</label>
		                    <label><input type="checkbox" name="type" value="부동산"/> 부동산</label>
		                    <label><input type="checkbox" name="type" value="기타"/> 기타</label>
		                </div>
		            </div>
		
		            <!-- 채널구분 -->
		            <div class="dropdown-box" id="channelBox">
		                <button type="button" class="dropdown-toggle">채널 구분 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="radio" name="channel" value="전체" checked/> 전체</label>
		                    <label><input type="radio" name="channel" value="온라인전용"/> 온라인전용</label>
		                </div>
		            </div>
		            
		            <!-- 수익률 정렬 -->
		    	    <div class="dropdown-box" id="rateBox">
		                <button type="button" class="dropdown-toggle">수익률 정렬 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="radio" name="interPeriod" value="1"/> 1개월</label>
		                    <label><input type="radio" name="interPeriod" value="3"/> 3개월</label>
		                    <label><input type="radio" name="interPeriod" value="6"/> 6개월</label>
		                    <label><input type="radio" name="interPeriod" value="12"/> 12개월</label>
		                    <label><input type="radio" name="interPeriod" value="100"/> 누적</label>
		                </div>
	            	</div>`; break;
			case '/commodity/tdf':
				searchBar.classList.add('visible');
				form.innerHTML = 
					`<!-- 위험등급 -->
		    	    <div class="dropdown-box" id="riskBox">
		                <button type="button" class="dropdown-toggle">위험등급 선택 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="checkbox" name="riskGrade" value="1"/> 매우높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="2"/> 높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="3"/> 다소높은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="4"/> 보통위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="5"/> 낮은위험</label>
		                    <label><input type="checkbox" name="riskGrade" value="6"/> 매우낮은위험</label>
		                </div>
		            </div>
		
		            <!-- 수익률 정렬 -->
		    	    <div class="dropdown-box" id="rateBox">
		                <button type="button" class="dropdown-toggle">수익률 정렬 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="radio" name="interPeriod" value="1"/> 1개월</label>
		                    <label><input type="radio" name="interPeriod" value="3"/> 3개월</label>
		                    <label><input type="radio" name="interPeriod" value="6"/> 6개월</label>
		                    <label><input type="radio" name="interPeriod" value="12"/> 12개월</label>
		                    <label><input type="radio" name="interPeriod" value="100"/> 누적</label>
		                </div>
	            	</div>`; break;
			case '/commodity/guarantee':
				searchBar.classList.add('visible');
				form.innerHTML = 
					`<!--예금 정렬 카테고리-->
		    	    <div class="dropdown-box" id="categoryBox">
		                <button type="button" class="dropdown-toggle">금리 정렬 ▼</button>
		                <div class="dropdown-options">
		                    <label><input type="radio" name="guaranteeCategory" value="db"/> DB금리순</label>
		                    <label><input type="radio" name="guaranteeCategory" value="dc"/> DC금리순</label>
		                    <label><input type="radio" name="guaranteeCategory" value="irp"/ >IRP금리순</label>
		                </div>
	            	</div>`; break;
	        default:
	        	form.innerHTML = '';
		        if (searchBar.classList.contains('visible')) {
		            searchBar.classList.remove('visible');
		        }
		}
		
		if(url != 'commodity/default') {
	    	// dropdowns open/close
	    	document.querySelectorAll('.dropdown-box').forEach(box => {
	    	    box.querySelector('.dropdown-toggle').addEventListener('click', () => box.classList.toggle('open'));
	    	});
	    	
	        /* search */
	        const searchBtn = qs('#searchBtn');
	        searchBtn.removeEventListener("click", handleSearch);
	        searchBtn.addEventListener('click', handleSearch);	
		}
		
	    clearMain();
	    postJson(url,body)
	        .then(renderJson)
	        .catch(err=>commodityMain.innerHTML=`<p style="color:red;">${err.message}</p>`); 

	};
	
	fetchAndRender('/commodity/fund', {channel:1, page:currentPage, size:pageSize}); //초기 랜더링

	function handleSearch() {
		
		document.querySelectorAll('.dropdown-box.open').forEach(box => box.classList.remove('open'));
		
	    const riskGrades = Array.from(document.querySelectorAll('input[name="riskGrade"]:checked')).map(e=>+e.value);
	    const types      = Array.from(document.querySelectorAll('input[name="type"]:checked')).map(e=>e.value);
	    const channelInp = qs('input[name="channel"]:checked');
	    const channel    = channelInp && channelInp.value==='온라인전용' ? 2 : 1;
	    const rateDom = qs('input[name="interPeriod"]:checked');
	    const rate = rateDom ? parseInt(rateDom.value) : 100;
	    const keywordDom = qs('#searchKeyword');
	    const keyword = keywordDom ? keywordDom.value : null;
	    const guaranteeCategory = qs('input[name="guaranteeCategory"]:checked');
	    const categoryVal = guaranteeCategory ? guaranteeCategory.value : null
	    
	    const body = {  
	    		channel,
				riskGrade: riskGrades.length?riskGrades:null,
				category: types.length?types:null,
				interPeriod:rate,
				keyword,
				page: 0,
				size: 10,
				guaranteeCategory: categoryVal
	    	  };
	    
	    let url='';
	    switch(currentTab){
	        case 'fund':      url='/commodity/fund'; break;
	        case 'etf':       url='/commodity/etf';  break;
	        case 'tdf':       url='/commodity/tdf';  break;
	        case 'default':   url='/commodity/default'; break;
	        case 'guarantee': url='/commodity/guarantee'; break;
	    }
	    searchAndRender(url, body);
	}

	// 검색시에 부르는 함수 --> 여기서 불러온 것을 기준으로 페이지네이션 되어야함.
	// 검색 시에는 폼 초기화 되면 안됨.
	function searchAndRender(url, body) {
		currentPage = 0; // 검색시 페이지는 초기화 되니까
		lastRequest = {url, body}; // 검색 후 상태를 기억해야함.
		
	    clearMain();
	    postJson(url,body)
	        .then(data => {
	        	renderJson(data);
	        })
	        .catch(err=>commodityMain.innerHTML=`<p style="color:red;">${err.message}</p>`); 
	}

	// 더보기 버튼을 눌렀을 시 호츌되는 함수
	function searchMoreAndRender(url, body) {
	    postJson(url,body)
	    .then(renderJson)
	    .catch(err=>commodityMain.innerHTML=`<p style="color:red;">${err.message}</p>`); 
	}

	qs('#loadMoreBtn').addEventListener('click', () => {
	    currentPage++;
	    lastRequest.body.page=currentPage; // 제일 최근 검색 요청에서 page값만 변경함
	 	// 더보기 목록 추가
	    searchMoreAndRender(lastRequest.url, lastRequest.body);
	});




	/*************************  CARD CLICK -> OPEN MODAL  *************************/
	commodityMain.addEventListener('click',e=>{
		
		if (!e.target.classList.contains('fund-name')) return; // 오직 펀드의 이름만 반응하도록

	    const card=e.target.closest('.fund-card');
	    const prodId = card?.dataset.prodid;
		
	    if (!prodId) return; // 유효성 검사
	    const fundName = e.target.textContent.trim();
	    
	    if(!card||!card.dataset.prodid) return; // guarantee/default cards have no prod analysis
	    openAnalysisModal(card.dataset.prodid, fundName);
	});

	function openAnalysisModal(prodId, title){
	    postJson('/commodity/analysis',{prodId})
	    .then(data=>{
	        qs('#modalTitle').textContent=title;
	        console.log(data.overviewUrl);
	        showModalWithUrl(data.overviewUrl);
	        // set active nav btns urls
	        qsa('#modalNav button').forEach(btn=>{
	            const key=btn.dataset.key;
	            btn.dataset.url = data[key]||'';
	            btn.classList.toggle('active', key==='overviewUrl');
	        });

	         // ▼ 모바일 드롭다운(navSelect) 세팅 ---------------
	         const navSelect = qs('#modalNavSelect');
	         if (window.matchMedia('(max-width:768px)').matches) {
	             navSelect.innerHTML = '';                      // 옵션 비우기
	             qsa('#modalNav button').forEach(btn => {
	                 const opt = document.createElement('option');
	                 opt.value = btn.dataset.url || '';
	                 opt.textContent = btn.textContent;
	                 if (btn.classList.contains('active')) opt.selected = true;
	                 navSelect.appendChild(opt);
	             });
	             navSelect.onchange = e => showModalWithUrl(e.target.value);
	             navSelect.hidden = false;                      // 모바일에서 표시

	         } else {
	             navSelect.hidden = true;                       // PC에서는 숨김
	         }
	         /* ──────────────────────────────────────────────── */
	        const files = [
	            ['file1Link','file1'],
	            ['file2Link','file2'],
	            ['file3Link','file3']
	        ];
	        files.forEach(([linkId,key])=>{
	            const linkEl = qs('#'+linkId);
	            if(data[key]){                // 값이 있으면 활성화
	            	const url = data[key];
	            	const fileName = url.substring(url.lastIndexOf('/')+1);
	            	const encoded = encodeURIComponent(fileName);
	            	const commonPart = url.substring(0, url.lastIndexOf('/')+1);
	            	const encodedUrl = commonPart + encoded;
	                linkEl.href = encodedUrl;
	                linkEl.classList.remove('disabled');
	            }else{                        // 없으면 비활성/회색
	                linkEl.href = '#';
	                linkEl.classList.add('disabled');
	            }
	        });
	        
	        qs('#analysisModal').classList.add('open');
	    });
	}

	function showModalWithUrl(url){
	    qs('#analysisFrame').src=url;
	}

	/*************************  MODAL NAV & CLOSE  *************************/
	qs('#modalNav').addEventListener('click',e=>{
	    if(e.target.tagName!=='BUTTON') return;
	    qsa('#modalNav button').forEach(b=>b.classList.remove('active'));
	    e.target.classList.add('active');
	    showModalWithUrl(e.target.dataset.url);
	});
	qs('#modalCloseBtn').addEventListener('click',()=>{
	    qs('#analysisModal').classList.remove('open');
	    qs('#analysisFrame').src='about:blank';
	});

	/* tab listeners */
	const tabMap = [
	    ['fundBtn','fund','/commodity/fund', 'fund'],
	    ['etfBtn','etf','/commodity/etf', 'fund'],
	    ['tdfBtn','tdf','/commodity/tdf', 'fund'],
	    ['defaultBtn','default','/commodity/default', 'default'],
	    ['guaranteeBtn','guarantee','/commodity/guarantee', 'guarantee'],
	];

	/*********************category buttons initialize************************/
	tabMap.forEach(([btnId,tab,url,status])=>{
	    qs('#'+btnId).addEventListener('click',()=>{
	        currentTab=tab;
	        setActiveTab(btnId);
	        if(status === 'fund') fetchAndRender(url,{channel:1, page:0, size: pageSize});
	        if(status === 'default') fetchAndRender(url, null);
	        if(status === 'guarantee') fetchAndRender(url, {page:0, size: pageSize, });
	    });
	});

	/* ③ 추가 – 터치/클릭 시 열려 있던 드롭다운 닫기 */
	document.addEventListener('click', e=>{
	  if(!e.target.closest('.dropdown-box')){
	    document.querySelectorAll('.dropdown-box.open')
	            .forEach(b=>b.classList.remove('open'));
	  }
	});

}

