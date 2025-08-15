// src/main/resources/static/js/purchase/holdings.js
(function(){
  const $ = (s)=>document.querySelector(s);
  const j = (o)=>JSON.stringify(o,null,2);
  const fmt = (v)=>{ if(v==null) return "-"; const n=Number(v); return isNaN(n)? String(v): n.toLocaleString(); };

  async function getJSON(url){
    const res = await fetch(url);
    if(!res.ok) throw new Error(`${res.status} ${url}`);
    return res.json();
  }
  async function postJSON(url, body){
    const res = await fetch(url,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)});
    if(!res.ok) throw new Error(`${res.status} ${url}`);
    return res.json();
  }

  function readAcct(){
    const accountType = $("#accountType")?.value || "IRP";
    const acountId    = $("#acountId")?.value?.trim();
    if(!acountId) throw new Error("acountId를 입력하세요.");
    return { accountType, acountId };
  }

  async function summary(){
    try{
      $("#sumStatus").textContent="조회중…";
      const {accountType, acountId} = readAcct();

      // 1) 요약
      const sum = await getJSON(`/purchase/api/holdings/summary?accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`);

      // 2) 거래내역 / 3) 입금내역
      await loadTxn();
      await loadDeposits();

      // 화면 표시(데모용 JSON)
      $("#summaryBox").textContent = j(sum);
      $("#sumStatus").textContent="완료";
    }catch(e){
      console.error(e); alert(e.message||"요약 조회 실패"); $("#sumStatus").textContent="오류";
    }
  }

  async function loadTxn(){
    const {accountType, acountId} = readAcct();
    const rows = await getJSON(`/purchase/api/holdings/transactions?accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`);
    const list = Array.isArray(rows)? rows : (rows.data||[]);
    const body = $("#txnBody");
    if(!list.length){ body.innerHTML=`<tr><td colspan="4">데이터 없음</td></tr>`; return; }
    body.innerHTML = list.map(r=>`
      <tr>
        <td>${r.txnDate || "-"}</td>
        <td>${r.productId || "-"}</td>
        <td>${r.txnType || "-"}</td>
        <td class="num">${fmt(r.txnAmt)}</td>
      </tr>
    `).join("");
  }

  async function loadDeposits(){
    const {accountType, acountId} = readAcct();
    const rows = await getJSON(`/purchase/api/holdings/deposits?accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`);
    const list = Array.isArray(rows)? rows : (rows.data||[]);
    const body = $("#depositBody");
    if(!list.length){ body.innerHTML=`<tr><td colspan="3">데이터 없음</td></tr>`; return; }
    body.innerHTML = list.map(r=>`
      <tr>
        <td>${r.depositDate || "-"}</td>
        <td>${r.description || "-"}</td>
        <td class="num">${fmt(r.depositAmount)}</td>
      </tr>
    `).join("");
  }

  async function deposit(){
    try{
      $("#depositStatus").textContent="처리중…";
      const {accountType, acountId} = readAcct();
      const amount = Number($("#depositAmount").value||0);
      const description = $("#depositDesc").value||"";
      if(!amount || amount<=0) throw new Error("입금 금액을 확인하세요.");

      const res = await postJSON(`/purchase/api/holdings/deposit`, { accountType, acountId, amount, description });
      $("#depositStatus").textContent="완료";
      await summary(); // 재조회
    }catch(e){
      console.error(e); alert(e.message||"입금 실패"); $("#depositStatus").textContent="오류";
    }
  }

  $("#loadSummaryBtn")?.addEventListener("click", summary);
  $("#depositBtn")?.addEventListener("click", deposit);
})();
