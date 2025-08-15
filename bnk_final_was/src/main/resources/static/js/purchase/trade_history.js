// src/main/resources/static/js/purchase/trade_history.js
// 매수예정상품 변경내역 조회 (TRANSACTION_HISTORY 기반)
// 호출 경로: /purchase/pending/history?accountType=IRP|DC&acountId=...

(function () {
  const $ = (s) => document.querySelector(s);

  function fmt(v) {
    if (v === undefined || v === null) return "-";
    const n = Number(v);
    return isNaN(n) ? String(v) : n.toLocaleString();
  }

  async function loadHistory() {
    const accountType = $("#accountType")?.value || "IRP";
    const acountId = $("#acountId")?.value?.trim();
    const from = $("#dateFrom")?.value?.trim(); // 선택사항(템플릿에 없으면 무시)
    const to = $("#dateTo")?.value?.trim();     // 선택사항

    if (!acountId) {
      alert("acountId를 입력하세요.");
      return;
    }

    setStatus("조회중…");

    try {
      const qs = new URLSearchParams({
        accountType,
        acountId,
      });
      if (from) qs.set("from", from);
      if (to) qs.set("to", to);

      const url = `/purchase/pending/history?${qs.toString()}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`GET ${url} → ${res.status}`);
      const json = await res.json();

      const rows = Array.isArray(json) ? json : (json.data || []);
      renderTable(rows);
      setStatus("완료");
    } catch (e) {
      console.error(e);
      setStatus("오류");
      alert("변경내역 조회 중 오류가 발생했습니다.");
    }
  }

  function renderTable(rows) {
    const body = $("#historyBody");
    if (!body) return;

    if (!rows.length) {
      body.innerHTML = `<tr><td colspan="4">데이터 없음</td></tr>`;
      return;
    }

    body.innerHTML = rows
      .map(
        (r) => `
      <tr>
        <td>${r.txnDate || "-"}</td>
        <td>${r.accountType || "-"}</td>
        <td>${r.productId || "-"} / ${r.productName || "-"}</td>
        <td class="num">${fmt(r.evalAmt)}</td>
      </tr>`
      )
      .join("");
  }

  function setStatus(text) {
    const el = $("#statusBadge");
    if (el) el.textContent = text;
  }

  // 이벤트 바인딩
  $("#loadBtn")?.addEventListener("click", loadHistory);

  // 엔터 키로도 조회
  $("#acountId")?.addEventListener("keydown", (e) => {
    if (e.key === "Enter") loadHistory();
  });
})();
