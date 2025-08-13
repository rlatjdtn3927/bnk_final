// src/main/resources/static/js/holdings.js
(function(){
  const $ = (sel) => document.querySelector(sel);
  const status = (t) => ($("#statusBadge").textContent = t);

  const qs = new URLSearchParams(location.search);
  if(qs.get("accountType")) $("#accountType").value = qs.get("accountType");
  if(qs.get("acountId")) $("#acountId").value = qs.get("acountId");

  function getKey(){
    return {
      accountType: $("#accountType").value || "IRP",
      acountId: ($("#acountId").value || "").trim()
    };
  }

  function fmt(v){
    if(v===undefined||v===null) return "-";
    try{
      const n = Number(v);
      if(isNaN(n)) return String(v);
      return n.toLocaleString();
    }catch(e){ return String(v); }
  }

  function renderSummary(data){
    const box = $("#summaryBox");
    if(!data || !data.data){ box.innerHTML = "<div class='kv'>데이터 없음</div>"; return; }
    const d = data.data;
    box.innerHTML = `
      <div class="kv"><span>총 평가액</span><strong>${fmt(d.totalEvalAmt)}</strong></div>
      <div class="kv"><span>수익률</span><strong>${fmt(d.totalProfitRate)}%</strong></div>
      <div class="kv"><span>누적 입금액</span><strong>${fmt(d.totalDepositAmt)}</strong></div>
      <div class="kv"><span>당일 입금액</span><strong>${fmt(d.todayDepositAmt)}</strong></div>
      <div class="kv"><span>운용 수익금액</span><strong>${fmt(d.opProfitAmt)}</strong></div>
    `;
  }

  function renderList(rows, kind){
    const head = $("#listHead");
    const body = $("#listBody");
    if(kind === "txn"){
      head.innerHTML = "<th>유형</th><th>금액/수량</th><th>일자</th>";
      if(!rows || !rows.data || rows.data.length===0){ body.innerHTML = "<tr><td colspan='3'>데이터 없음</td></tr>"; return; }
      body.innerHTML = rows.data.map(r => `
        <tr>
          <td>${r.txnType || "-"}</td>
          <td>${fmt(r.txnAmt)} / ${fmt(r.quantity)}</td>
          <td>${r.txnDate || "-"}</td>
        </tr>
      `).join("");
    }else{
      head.innerHTML = "<th>금액</th><th>잔액</th><th>일시</th>";
      if(!rows || !rows.data || rows.data.length===0){ body.innerHTML = "<tr><td colspan='3'>데이터 없음</td></tr>"; return; }
      body.innerHTML = rows.data.map(r => `
        <tr>
          <td>${fmt(r.depositAmount)}</td>
          <td>${fmt(r.balanceAfter)}</td>
          <td>${r.depositDate || "-"}</td>
        </tr>
      `).join("");
    }
  }

  async function fetchJSON(url, init){
    const res = await fetch(url, init);
    if(!res.ok) throw new Error(`${init?.method||'GET'} ${url} → ${res.status}`);
    return res.json();
  }

  async function reloadSummaryAndDeposits(){
    const key = getKey();
    const [sumJson, depJson] = await Promise.all([
      fetchJSON(`/holdings/summary?accountType=${encodeURIComponent(key.accountType)}&acountId=${encodeURIComponent(key.acountId)}`),
      fetchJSON(`/holdings/deposits?accountType=${encodeURIComponent(key.accountType)}&acountId=${encodeURIComponent(key.acountId)}`)
    ]);
    renderSummary(sumJson);
    renderList(depJson, "deposit");
  }

  // 이벤트
  $("#loadSummary").addEventListener("click", async ()=>{
    const key = getKey();
    if(!key.acountId){ alert("acountId를 입력하세요."); return; }
    status("요약 조회중…");
    try{
      const json = await fetchJSON(`/holdings/summary?accountType=${encodeURIComponent(key.accountType)}&acountId=${encodeURIComponent(key.acountId)}`);
      renderSummary(json);
      status("완료");
    }catch(e){ console.error(e); status("오류"); $("#summaryBox").innerHTML = "<div class='kv'>오류</div>"; }
  });

  $("#loadTxns").addEventListener("click", async ()=>{
    const key = getKey();
    if(!key.acountId){ alert("acountId를 입력하세요."); return; }
    status("거래내역 조회중…");
    try{
      const json = await fetchJSON(`/holdings/transactions?accountType=${encodeURIComponent(key.accountType)}&acountId=${encodeURIComponent(key.acountId)}`);
      renderList(json, "txn");
      status("완료");
    }catch(e){ console.error(e); status("오류"); }
  });

  $("#loadDeposits").addEventListener("click", async ()=>{
    const key = getKey();
    if(!key.acountId){ alert("acountId를 입력하세요."); return; }
    status("입금내역 조회중…");
    try{
      const json = await fetchJSON(`/holdings/deposits?accountType=${encodeURIComponent(key.accountType)}&acountId=${encodeURIComponent(key.acountId)}`);
      renderList(json, "deposit");
      status("완료");
    }catch(e){ console.error(e); status("오류"); }
  });

  $("#depositBtn").addEventListener("click", async ()=>{
    const key = getKey();
    const amount = Number($("#depositAmt").value||"0");
    const description = ($("#depositDesc").value||"").trim();
    if(!key.acountId){ alert("acountId를 입력하세요."); return; }
    if(!(amount>0)){ alert("입금 금액을 입력하세요."); return; }
    status("입금 처리중…");
    try{
      const body = { accountType:key.accountType, acountId:key.acountId, amount, description };
      const json = await fetchJSON("/holdings/deposit", {
        method:"POST",
        headers:{ "Content-Type":"application/json" },
        body: JSON.stringify(body)
      });
      $("#depositResult").textContent = JSON.stringify(json, null, 2);
      await reloadSummaryAndDeposits(); // 즉시 반영
      status("완료");
    }catch(e){ console.error(e); status("오류"); }
  });
})();
