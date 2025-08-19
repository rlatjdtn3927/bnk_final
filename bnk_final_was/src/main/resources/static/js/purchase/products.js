/* -------------------------------------------------------------------
 * products.js — 상품관리 탭 (단일 파일)
 *
 * 역할
 * 1) 보유상품 목록 조회  : GET  /purchase/api/portfolio
 * 2) 변경내역 조회       : GET  /purchase/api/trade/pending/history
 * 3) 보유변경/만기예약/매수예정 위저드 진입(별도 페이지 step1로 이동)
 * ------------------------------------------------------------------ */
(function(){
  const $  = (s, el=document)=> el.querySelector(s);
  const fmt = n => (n===null||n===undefined) ? "-" : Number(n).toLocaleString();

  /* ------------ 공통: 계좌 파라미터 읽기 + 검증 ------------- */
  function readAccountOrThrow() {
    const accountType = $('#accountType')?.value?.trim();
    const acountId    = $('#acountId')?.value?.trim();
    if(!accountType) throw new Error('계좌유형을 선택하세요.');
    if(!acountId)    throw new Error('acountId를 입력하세요.');
    return { accountType, acountId };
  }

  /* ---------------- 1) 보유상품 목록 ------------------------ */
  async function loadPortfolio(){
    try{
      $('#statusBadge').textContent = '조회중…';
      const { accountType, acountId } = readAccountOrThrow();
      const qs = new URLSearchParams({accountType, acountId}).toString();
      const res = await fetch(`/purchase/api/portfolio?${qs}`);
      if(!res.ok) throw new Error('보유목록 호출 실패');
      const list = await res.json();

      const tbody = $('#portfolioBody');
      tbody.innerHTML = '';
      if(!list || list.length===0){
        tbody.innerHTML = '<tr><td colspan="4">데이터 없음</td></tr>';
      }else{
        list.forEach(it=>{
          const tr = document.createElement('tr');
          tr.innerHTML =
            `<td>${it.productId ?? '-'}</td>
             <td>${it.productType ?? '-'}</td>
             <td class="num">${it.quantity ?? '-'}</td>
             <td class="num">${fmt(it.evalAmt)}</td>`;
          tbody.appendChild(tr);
        });
      }
      $('#statusBadge').textContent = '완료';
    }catch(e){
      console.error(e);
      $('#statusBadge').textContent = '오류';
      alert(e.message || '보유상품 조회 중 오류');
    }
  }

  /* ---------------- 2) 변경내역 조회 ------------------------ */
  async function loadHistory(){
    try{
      $('#histStatus').textContent = '조회중…';
      const { accountType, acountId } = readAccountOrThrow();
      const from = $('#histFrom')?.value || '';
      const to   = $('#histTo')?.value || '';
      const qs = new URLSearchParams({ accountType, acountId, from, to }).toString();
      const res = await fetch(`/purchase/api/trade/pending/history?${qs}`);
      if(!res.ok) throw new Error('변경내역 호출 실패');
      const rows = await res.json();

      const tbody = $('#historyBody');
      tbody.innerHTML = '';
      if(!rows || rows.length===0){
        tbody.innerHTML = '<tr><td colspan="4">데이터 없음</td></tr>';
      }else{
        rows.forEach(r=>{
          const tr = document.createElement('tr');
          tr.innerHTML =
            `<td>${r.txnDate ?? '-'}</td>
             <td>${r.accountType ?? '-'}</td>
             <td>${(r.productId ?? '-') + ' / ' + (r.productName ?? '-')}</td>
             <td class="num">${fmt(r.evalAmt)}</td>`;
          tbody.appendChild(tr);
        });
      }
      $('#histStatus').textContent = '완료';
    }catch(e){
      console.error(e);
      $('#histStatus').textContent = '오류';
      alert(e.message || '변경내역 조회 중 오류');
    }
  }

  /* ---------------- 3) 위저드 진입 -------------------------- */
  function gotoFlow(flow){
    location.href = `/purchase/trade?flow=${encodeURIComponent(flow)}`;
  }

  /* ---------------- 바인딩 ------------------------------- */
  $('#loadPortfolio')?.addEventListener('click', loadPortfolio);
  $('#loadHistoryBtn')?.addEventListener('click', loadHistory);
  $('#acountId')?.addEventListener('keydown', (e)=>{ if(e.key==='Enter') loadPortfolio(); });

  $('#goChange')?.addEventListener('click', ()=>gotoFlow('CHANGE'));
  $('#goMaturity')?.addEventListener('click', ()=>gotoFlow('MATURITY'));
  $('#goPending')?.addEventListener('click', ()=>gotoFlow('PENDING'));
})();
