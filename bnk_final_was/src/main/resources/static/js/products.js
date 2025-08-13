// src/main/resources/static/js/products.js
(function(){
  const $ = (sel)=>document.querySelector(sel);
  const status = (t)=>($("#statusBadge").textContent=t);

  const qs = new URLSearchParams(location.search);
  if(qs.get("accountType")) $("#accountType").value = qs.get("accountType");
  if(qs.get("acountId")) $("#acountId").value = qs.get("acountId");

  function getKey(){
    return {
      accountType: $("#accountType").value || "IRP",
      acountId: ($("#acountId").value||"").trim()
    };
  }

  function renderPortfolio(list){
    const body = $("#portfolioBody");
    if(!list || !list.data || list.data.length===0){
      body.innerHTML = "<tr><td colspan='4'>데이터 없음</td></tr>";
      return;
    }
    body.innerHTML = list.data.map(r=>`
      <tr>
        <td>${r.productId||"-"}</td>
        <td>${r.productType||"-"}</td>
        <td>${r.quantity??"-"}</td>
        <td>${r.evalAmt??"-"}</td>
      </tr>
    `).join("");
  }

  $("#loadPortfolio").addEventListener("click", async ()=>{
    const key = getKey();
    if(!key.acountId){ alert("acountId를 입력하세요."); return; }
    status("보유상품 조회중…");
    try{
      const res = await fetch(`/portfolio?accountType=${encodeURIComponent(key.accountType)}&acountId=${encodeURIComponent(key.acountId)}`);
      const json = await res.json();
      renderPortfolio(json);
      status("완료");
    }catch(e){ console.error(e); status("오류"); }
  });

  $("#previewBtn").addEventListener("click", async ()=>{
    const key = getKey();
    if(!key.acountId){ alert("acountId를 입력하세요."); return; }
    let lines = [];
    try{ lines = JSON.parse($("#changeLines").value||"[]"); }catch(e){ alert("JSON 형식이 올바르지 않습니다."); return; }
    status("미리보기…");
    try{
      const res = await fetch("/portfolio/change/preview", {
        method:"POST", headers:{ "Content-Type":"application/json" },
        body: JSON.stringify({ accountType:key.accountType, acountId:key.acountId, lines })
      });
      const json = await res.json();
      $("#changeResult").textContent = JSON.stringify(json, null, 2);
      status("완료");
    }catch(e){ console.error(e); status("오류"); }
  });

  $("#applyBtn").addEventListener("click", async ()=>{
    const key = getKey();
    if(!key.acountId){ alert("acountId를 입력하세요."); return; }
    let lines = [];
    try{ lines = JSON.parse($("#changeLines").value||"[]"); }catch(e){ alert("JSON 형식이 올바르지 않습니다."); return; }
    if(!confirm("변경 내용을 즉시 적용합니다. 진행할까요?")) return;
    status("적용중…");
    try{
      const res = await fetch("/portfolio/change/apply", {
        method:"POST", headers:{ "Content-Type":"application/json" },
        body: JSON.stringify({ accountType:key.accountType, acountId:key.acountId, lines })
      });
      const json = await res.json();
      $("#changeResult").textContent = JSON.stringify(json, null, 2);

      // 즉시 반영: 최신 보유목록 재조회
      const res2 = await fetch(`/portfolio?accountType=${encodeURIComponent(key.accountType)}&acountId=${encodeURIComponent(key.acountId)}`);
      const list = await res2.json();
      renderPortfolio(list);

      status("완료");
    }catch(e){ console.error(e); status("오류"); }
  });

})();
