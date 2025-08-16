// src/main/resources/static/js/purchase/trade_history.js
(function () {
  const $ = (s) => document.querySelector(s);
  const fmt = (v)=>{ if(v==null) return "-"; const n=Number(v); return isNaN(n)? String(v): n.toLocaleString(); };

  async function getJSON(url){
    const res = await fetch(url);
    if(!res.ok) throw new Error(`${res.status} ${url}`);
    return res.json();
  }

  async function loadHistory() {
    const accountType = $("#accountType")?.value || "IRP";
    const acountId = $("#acountId")?.value?.trim();
    const from = $("#dateFrom")?.value?.trim();
    const to   = $("#dateTo")?.value?.trim();
    if (!acountId) { alert("acountId를 입력하세요."); return; }

    setStatus("조회중…");
    try {
      const qs = new URLSearchParams({ accountType, acountId });
      if (from) qs.set("from", from);
      if (to) qs.set("to", to);
      // 위저드 API 네임스페이스 사용
      const rows = await getJSON(`/purchase/api/trade/pending/history?${qs.toString()}`);
      const list = Array.isArray(rows) ? rows : (rows.data || []);
      renderTable(list);
      setStatus("완료");
    } catch (e) {
      console.error(e);
      setStatus("오류");
      alert("변경내역 조회 중 오류가 발생했습니다.");
    }
  }

  function renderTable(rows) {
    const body = $("#historyBody");
    if (!rows.length) { body.innerHTML = `<tr><td colspan="4">데이터 없음</td></tr>`; return; }
    body.innerHTML = rows.map((r)=>`
      <tr>
        <td>${r.txnDate || "-"}</td>
        <td>${r.accountType || "-"}</td>
        <td>${r.productId || "-"} / ${r.productName || "-"}</td>
        <td class="num">${fmt(r.evalAmt)}</td>
      </tr>`).join("");
  }

  function setStatus(text) {
    const el = $("#statusBadge");
    if (el) el.textContent = text;
  }

  $("#loadBtn")?.addEventListener("click", loadHistory);
  $("#acountId")?.addEventListener("keydown", (e) => { if (e.key === "Enter") loadHistory(); });
})();
