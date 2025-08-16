/* -------------------------------------------------------------------
 * products.js — 상품관리 탭
 *
 * 기능
 * 1) 업무 타일 클릭 → /purchase/trade?flow=... (스텝1로 이동)
 * 2) 보유상품 조회 → GET /purchase/api/portfolio
 *
 * 주의
 * - "보유상품 즉시 변경" UI는 제거(기획 반영).
 * - 에러/로딩 상태는 상단 뱃지로 표시.
 * ------------------------------------------------------------------ */

(function () {
  'use strict';

  const $ = (s) => document.querySelector(s);
  const qs = (o) => new URLSearchParams(o).toString();

  /* --------- 업무 선택 → 위저드 스텝1로 --------- */
  function gotoFlow(flow) {
    // PageController.tradeEntry 가 /purchase/trade/{FLOW}/step1 로 리다이렉트함
    location.href = `/purchase/trade?flow=${encodeURIComponent(flow)}`;
  }

  /* --------- 보유상품 목록 조회 --------- */
  async function loadPortfolio() {
    const accountType = $('#accountType')?.value?.trim() ?? '';
    const acountId = $('#acountId')?.value?.trim() ?? '';

    if (!accountType || !acountId) {
      alert('계좌유형과 acountId를 입력하세요.');
      return;
    }

    const badge = $('#statusBadge');
    if (badge) badge.textContent = '불러오는 중...';

    try {
      const url = `/purchase/api/portfolio?` + qs({ accountType, acountId });
      const res = await fetch(url);
      if (!res.ok) throw new Error('목록 호출 실패');

      const list = await res.json(); // [{productId,productType,quantity,evalAmt,...}]
      const tbody = $('#portfolioBody');
      tbody.innerHTML = '';

      if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">데이터 없음</td></tr>';
      } else {
        for (const it of list) {
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td>${it.productId ?? '-'}</td>
            <td>${it.productType ?? '-'}</td>
            <td class="num">${it.quantity ?? 0}</td>
            <td class="num">${it.evalAmt ?? 0}</td>`;
          tbody.appendChild(tr);
        }
      }
      if (badge) badge.textContent = '완료';
    } catch (e) {
      console.error(e);
      if (badge) badge.textContent = '실패';
      alert(e.message || '보유상품 조회 중 오류가 발생했습니다.');
    }
  }

  /* --------- 이벤트 바인딩 --------- */
  window.addEventListener('DOMContentLoaded', () => {
    $('#goChange')?.addEventListener('click', () => gotoFlow('CHANGE'));
    $('#goMaturity')?.addEventListener('click', () => gotoFlow('MATURITY'));
    $('#goPending')?.addEventListener('click', () => gotoFlow('PENDING'));

    $('#loadPortfolio')?.addEventListener('click', loadPortfolio);
    $('#acountId')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        loadPortfolio();
      }
    });
  });
})();
