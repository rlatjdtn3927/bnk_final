// src/main/resources/static/js/purchase/holdings.js
(function(){
  const $=(s)=>document.querySelector(s);
  const fmt=(v)=>{ if(v==null) return "-"; const n=Number(v); return isNaN(n)?String(v):n.toLocaleString(); };

  async function summary(){
    const accountType=$("#accountType").value;
    const acountId=$("#acountId").value.trim();
    if(!acountId){ alert("acountId를 입력하세요."); return; }
    $("#sumStatus").textContent="조회중…";
    try{
      const res=await fetch(`/purchase/portfolio?accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`);
      const port=await res.json();
      // 요약 정보는 SUMMARY_GET을 붙여도 되지만, 우선 포트폴리오/내역을 함께 보이도록 간단히 처리
      $("#summaryBox").textContent = JSON.stringify({portfolio:port}, null, 2);
      await loadTxn(); await loadDeposits();
      $("#sumStatus").textContent="완료";
    }catch(e){ console.error(e); $("#sumStatus").textContent="오류"; }
  }

  async function loadTxn(){
    const accountType=$("#accountType").value;
    const acountId=$("#acountId").value.trim();
    const res=await fetch(`/purchase/txns?accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`);
    const rows=await res.json();
    const list=Array.isArray(rows)?rows:(rows.data||[]);
    const body=$("#txnBody");
    if(!list.length){ body.innerHTML=`<tr><td colspan="4">데이터 없음</td></tr>`; return; }
    body.innerHTML=list.map(r=>`
      <tr>
        <td>${(r.txnDate||"").toString().replace("T"," ")}</td>
        <td>${r.productId||"-"}</td>
        <td>${r.txnType||"-"}</td>
        <td class="num">${fmt(r.txnAmt)}</td>
      </tr>`).join("");
  }

  async function loadDeposits(){
    const accountType=$("#accountType").value;
    const acountId=$("#acountId").value.trim();
    const res=await fetch(`/purchase/deposits?accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`);
    const rows=await res.json();
    const list=Array.isArray(rows)?rows:(rows.data||[]);
    const body=$("#depositBody");
    if(!list.length){ body.innerHTML=`<tr><td colspan="3">데이터 없음</td></tr>`; return; }
    body.innerHTML=list.map(r=>`
      <tr>
        <td>${(r.depositDate||"").toString().replace("T"," ")}</td>
        <td>${r.description||"-"}</td>
        <td class="num">${fmt(r.depositAmount)}</td>
      </tr>`).join("");
  }

  async function deposit(){
    const accountType=$("#accountType").value;
    const acountId=$("#acountId").value.trim();
    const amount=Number($("#depositAmount").value||0);
    const description=$("#depositDesc").value||"";
    if(!acountId||amount<=0){ alert("계좌/금액 확인"); return; }
    $("#depositStatus").textContent="처리중…";
    try{
      const res=await fetch("/purchase/deposit",{method:"POST",headers:{"Content-Type":"application/json"},
        body:JSON.stringify({accountType, acountId, amount, description})});
      const json=await res.json();
      $("#depositStatus").textContent="완료";
      await summary(); // 재조회
    }catch(e){ console.error(e); $("#depositStatus").textContent="오류"; }
  }

  $("#loadSummaryBtn")?.addEventListener("click", summary);
  $("#depositBtn")?.addEventListener("click", deposit);
})();
