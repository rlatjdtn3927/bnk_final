// src/main/resources/static/js/purchase/trade_wizard.js
(function(){
  const $ = (s)=>document.querySelector(s);
  const API = "/purchase/api/trade"; // ← 위저드 전용 API 네임스페이스
  const qs = new URLSearchParams(location.search);
  const flow = (qs.get("flow")||"PENDING").toUpperCase(); // PENDING | CHANGE | MATURITY
  const chosen = []; // { productType, productId?, name, ratio }

  const setBadge = (sel, t)=>{ const el=$(sel); if(el) el.textContent=t; };
  const ratioSum = ()=> chosen.reduce((a,c)=>a+(Number(c.ratio)||0),0);

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

  setBadge("#flowBadge", flow);

  // STEP 0: 사전 체크
  async function prereq(){
    const userId     = $("#userId")?.value?.trim();
    const accountType= $("#accountType")?.value || "IRP";
    const acountId   = $("#acountId")?.value?.trim();
    if(!userId || !acountId){ alert("userId / acountId 입력"); return null; }

    setBadge("#statusBadge","체크중…");
    try{
      const url = `${API}/check?userId=${encodeURIComponent(userId)}&accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`;
      const json = await getJSON(url);
      $("#checkResult").textContent = JSON.stringify(json,null,2);

      if(accountType==="IRP" && json && json.hasIrpAccount===false){
        if(confirm("IRP 계좌가 없습니다. 계좌 개설 페이지로 이동할까요?")) location.href="/purchase/account/open";
      }
      if(json && (!json.hasBalance || Number(json.balance||0)<=0)){
        if(confirm("잔액이 0원입니다. 입금 페이지로 이동할까요?")) location.href="/purchase/holdings";
      }

      setBadge("#statusBadge","완료");
      return json;
    }catch(e){ console.error(e); setBadge("#statusBadge","오류"); alert("사전 체크 오류"); return null; }
  }
  $("#checkBtn")?.addEventListener("click", prereq);
  $("#goStep1")?.addEventListener("click", async ()=>{ const ok = await prereq(); if(ok) window.scrollTo({top:document.body.scrollHeight,behavior:"smooth"}); });

  // STEP 1: 상품 검색/추가(+현금성)
  async function searchProducts(){
    const cat=$("#cat")?.value||"FUND"; const q=$("#q")?.value?.trim()||"";
    setBadge("#statusBadge","검색중…");
    try{
      const list = await getJSON(`${API}/products?category=${encodeURIComponent(cat)}&q=${encodeURIComponent(q)}`);
      const rows = Array.isArray(list)? list : (list.data||[]);
      const body = $("#searchBody");
      if(!rows.length){ body.innerHTML=`<tr><td colspan="3">검색 결과 없음</td></tr>`; }
      else{
        body.innerHTML = rows.map(it=>`
          <tr>
            <td>${it.productId || "-"}</td>
            <td>${it.productName || it.name || "-"}</td>
            <td><button class="btn" data-add='${JSON.stringify(it).replace(/'/g,"&#39;")}'>추가</button></td>
          </tr>`).join("");
        body.querySelectorAll("button[data-add]").forEach(btn=>{
          btn.addEventListener("click", ()=>{
            const it = JSON.parse(btn.getAttribute("data-add"));
            const cat2 = it.category || cat;
            if(cat2==="CASH"){ alert("현금성은 '현금성 추가' 사용"); return; }
            if(chosen.find(c=>c.productId===it.productId)) { alert("이미 추가됨"); return; }
            chosen.push({ productType: cat2, productId: it.productId, name: it.productName || it.name || it.productId, ratio:0 });
            renderChosen();
          });
        });
      }
      setBadge("#statusBadge","완료");
    }catch(e){ console.error(e); setBadge("#statusBadge","오류"); alert("상품 검색 오류"); }
  }
  $("#searchBtn")?.addEventListener("click", searchProducts);
  $("#addCashBtn")?.addEventListener("click", ()=>{
    if(chosen.find(c=>c.productType==="CASH")){ alert("현금성은 1개만 추가"); return; }
    chosen.push({ productType:"CASH", productId:null, name:"현금성 자산", ratio:0 });
    renderChosen();
  });

  function renderChosen(){
    const body=$("#chosenBody");
    if(!chosen.length){ body.innerHTML=`<tr><td colspan="4">선택 없음</td></tr>`; setBadge("#ratioSum","0%"); return; }
    body.innerHTML = chosen.map((c,i)=>`
      <tr>
        <td>${c.productId || "-"}</td>
        <td><span class="badge">${c.productType}</span> ${c.name||"-"}</td>
        <td><input type="number" min="0" max="100" step="1" class="input" data-idx="${i}" value="${c.ratio}"/></td>
        <td><button class="btn" data-del="${i}">삭제</button></td>
      </tr>
    `).join("");
    body.querySelectorAll("input[data-idx]").forEach(inp=>{
      inp.addEventListener("input",()=>{ const i=Number(inp.getAttribute("data-idx")); chosen[i].ratio = Number(inp.value||0); setBadge("#ratioSum",`${ratioSum()}%`); });
    });
    body.querySelectorAll("button[data-del]").forEach(btn=>{
      btn.addEventListener("click",()=>{ const i=Number(btn.getAttribute("data-del")); chosen.splice(i,1); renderChosen(); });
    });
    setBadge("#ratioSum",`${ratioSum()}%`);
  }

  $("#goStep2")?.addEventListener("click", async ()=>{
    if(!chosen.length){ alert("상품 선택"); return; }
    if(ratioSum()!==100){ alert("비율 합계 100% 필요"); return; }
    try{
      const items = chosen.filter(c=>c.productType!=="CASH").map(c=>({productType:c.productType, productId:c.productId}));
      const docs  = await postJSON(`${API}/documents`, { items });
      const list  = Array.isArray(docs)? docs : (docs.data||[]);
      const box   = $("#docBox");
      if(!list.length) box.innerHTML = "동의할 서류 없음";
      else box.innerHTML = list.map(d=>`
        <div class="row">
          <label><input type="checkbox" data-doc="${d.docId||d.fileUrl}"/> ${d.docType||"서류"}</label>
          <a class="btn ghost" href="${d.fileUrl||'#'}" target="_blank">열기</a>
        </div>
      `).join("");
      window.scrollTo({top:document.body.scrollHeight,behavior:"smooth"});
    }catch(e){ console.error(e); alert("서류 로드 오류"); }
  });

  $("#goStep3")?.addEventListener("click", async ()=>{
    const checks = Array.from(document.querySelectorAll("#docBox input[type='checkbox']"));
    const allAgree = checks.length===0 || checks.every(ch=>ch.checked);
    if(!allAgree){ alert("모든 서류 동의 필요"); return; }

    const userId=$("#userId")?.value?.trim(); const accountType=$("#accountType")?.value||"IRP"; const acountId=$("#acountId")?.value?.trim();
    if(!userId || !acountId){ alert("userId / acountId 입력"); return; }
    if(ratioSum()!==100){ alert("비율 합계 100% 필요"); return; }

    try{
      const payload = { userId, flow, accountType, acountId, items: chosen };
      const json = await postJSON(`${API}/preview`, payload);
      $("#beforeBox").textContent = JSON.stringify(json.before||{},null,2);
      $("#afterBox").textContent  = JSON.stringify(json.after||{},null,2);
      window.scrollTo({top:document.body.scrollHeight,behavior:"smooth"});
    }catch(e){ console.error(e); alert("미리보기 오류"); }
  });

  $("#applyBtn")?.addEventListener("click", async ()=>{
    const userId=$("#userId")?.value?.trim(); const accountType=$("#accountType")?.value||"IRP"; const acountId=$("#acountId")?.value?.trim(); const acctPwd=$("#acctPwd")?.value||"";
    if(!userId || !acountId){ alert("userId / acountId 입력"); return; }
    if(accountType==="IRP" && !acctPwd){ alert("IRP 비밀번호 입력"); return; }
    if(ratioSum()!==100){ alert("비율 합계 100% 필요"); return; }

    setBadge("#applyStatus","적용중…");
    try{
      const payload = { userId, flow, accountType, acountId, items: chosen, acctPwd };
      const res = await postJSON(`${API}/apply`, payload);
      $("#doneBox").textContent = JSON.stringify(res,null,2);
      setBadge("#applyStatus","완료");
      window.scrollTo({top:document.body.scrollHeight,behavior:"smooth"});
    }catch(e){ console.error(e); setBadge("#applyStatus","오류"); alert("적용 오류"); }
  });
})();
