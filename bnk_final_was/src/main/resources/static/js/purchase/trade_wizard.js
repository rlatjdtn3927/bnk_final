// src/main/resources/static/js/purchase/trade_wizard.js
// 구매 프로세스 공통 위저드 JS
// 흐름: 사전체크 → 상품선택(펀드/예금/현금성) → 서류 동의 → 미리보기 → 비밀번호 확인/적용(즉시)

// ────────────────────────────────────────────────────────────────
// 유틸
// ────────────────────────────────────────────────────────────────
(function(){
  const $ = (s)=>document.querySelector(s);
  const qs = new URLSearchParams(location.search);
  const flow = (qs.get("flow")||"PENDING").toUpperCase(); // PENDING | CHANGE | MATURITY
  const chosen = []; // { productType: 'FUND'|'ETF'|'TDF'|'PRINCIPAL'|'CASH', productId?: string, name: string, ratio: number }

  const state = {
    prereq: null,   // 사전체크 결과 캐시
    docs: [],       // 동의 서류 목록 캐시
  };

  const fmt = (v)=>{
    if(v===undefined||v===null) return "-";
    const n = Number(v);
    return isNaN(n)? String(v) : n.toLocaleString();
  };

  const ok = (x)=> !!x;
  const ratioSum = ()=> chosen.reduce((a,c)=>a+(Number(c.ratio)||0),0);

  const setBadge = (id, text)=> { const el = $(id); if(el) el.textContent = text; };

// ────────────────────────────────────────────────────────────────
  // 초기 뱃지
  setBadge("#flowBadge", flow);

// ────────────────────────────────────────────────────────────────
// STEP 0 — 사전 체크
// ────────────────────────────────────────────────────────────────
  async function prereq(){
    const userId     = $("#userId")?.value?.trim();
    const accountType= $("#accountType")?.value || "IRP";
    const acountId   = $("#acountId")?.value?.trim();

    if(!userId || !acountId){
      alert("userId / acountId 를 입력하세요.");
      return null;
    }

    setBadge("#statusBadge", "체크중…");
    try{
      // /purchase 네임스페이스로 호출
      const url = `/purchase/check?userId=${encodeURIComponent(userId)}&accountType=${encodeURIComponent(accountType)}&acountId=${encodeURIComponent(acountId)}`;
      const res = await fetch(url);
      if(!res.ok) throw new Error(`GET ${url} → ${res.status}`);
      const json = await res.json();
      state.prereq = json;

      const box = $("#checkResult");
      if(box) box.textContent = JSON.stringify(json, null, 2);

      // 계좌 개설/입금 유도는 알림으로만 안내(유효성은 나중에 강화)
      if(accountType === "IRP"){
        if(json && json.hasIrpAccount === false){
          if(confirm("IRP 계좌가 없습니다. (유효성 검사는 나중에 연결) 계좌 개설 페이지로 이동할까요?")) {
            location.href = "/purchase/account/open";
            return null;
          }
        }
      }
      if(json && (!json.hasBalance || Number(json.balance||0) <= 0)){
        if(confirm("계좌 잔액이 0원입니다. 입금 페이지로 이동할까요?")) {
          location.href = "/purchase/holdings"; // 이후 템플릿에서 이 라우트에 맞춰 연결
          return null;
        }
      }

      setBadge("#statusBadge", "완료");
      return json;
    }catch(e){
      console.error(e);
      setBadge("#statusBadge", "오류");
      alert("사전 체크 호출 중 오류가 발생했습니다.");
      return null;
    }
  }

  $("#checkBtn")?.addEventListener("click", prereq);

  $("#goStep1")?.addEventListener("click", async ()=>{
    const res = await prereq();
    if(!res) return;
    window.scrollTo({top: document.body.scrollHeight, behavior:"smooth"});
  });

// ────────────────────────────────────────────────────────────────
// STEP 1 — 상품 검색/선택(현금성 추가 포함)
// ────────────────────────────────────────────────────────────────
  async function searchProducts(){
    const cat = $("#cat")?.value || "FUND";
    const q   = $("#q")?.value?.trim() || "";

    setBadge("#statusBadge", "검색중…");
    try{
      const url = `/purchase/products?category=${encodeURIComponent(cat)}&q=${encodeURIComponent(q)}`;
      const res = await fetch(url);
      if(!res.ok) throw new Error(`GET ${url} → ${res.status}`);
      const json = await res.json();
      const list = Array.isArray(json) ? json : (json.data||[]);
      const body = $("#searchBody");
      if(!body) return;

      if(!list.length){
        body.innerHTML = `<tr><td colspan="3">검색 결과 없음</td></tr>`;
      }else{
        body.innerHTML = list.map(it=>`
          <tr>
            <td>${it.productId || "-"}</td>
            <td>${it.productName || it.name || "-"} ${it.meta?`<span class="badge">${it.meta}</span>`:""}</td>
            <td><button class="btn" data-add='${JSON.stringify(it).replace(/'/g, '&#39;')}'>추가</button></td>
          </tr>
        `).join("");
        body.querySelectorAll("button[data-add]").forEach(btn=>{
          btn.addEventListener("click", ()=>{
            const it = JSON.parse(btn.getAttribute("data-add"));
            if(cat === "CASH"){ alert("현금성은 아래 '현금성 추가' 버튼을 이용하세요."); return; }
            if(chosen.find(c=>c.productId===it.productId)){ alert("이미 추가된 상품입니다."); return; }
            chosen.push({
              productType: it.category || cat,
              productId  : it.productId,
              name       : it.productName || it.name || it.productId,
              ratio      : 0
            });
            renderChosen();
          });
        });
      }
      setBadge("#statusBadge", "완료");
    }catch(e){
      console.error(e);
      setBadge("#statusBadge", "오류");
      alert("상품 검색 중 오류가 발생했습니다.");
    }
  }
  $("#searchBtn")?.addEventListener("click", searchProducts);

  $("#addCashBtn")?.addEventListener("click", ()=>{
    if(chosen.find(c=>c.productType==="CASH")){
      alert("현금성 자산은 1개만 추가할 수 있습니다.");
      return;
    }
    chosen.push({ productType:"CASH", productId:null, name:"현금성 자산", ratio:0 });
    renderChosen();
  });

  function renderChosen(){
    const body = $("#chosenBody");
    if(!body) return;

    if(!chosen.length){
      body.innerHTML = `<tr><td colspan="4">선택 없음</td></tr>`;
      setBadge("#ratioSum", "0%");
      return;
    }

    body.innerHTML = chosen.map((c,idx)=>`
      <tr>
        <td>${c.productId || "-"}</td>
        <td><span class="badge">${c.productType}</span> ${c.name || "-"}</td>
        <td>
          <input type="number" min="0" max="100" step="1" data-idx="${idx}" class="input" value="${c.ratio}"/>
        </td>
        <td><button class="btn" data-del="${idx}">삭제</button></td>
      </tr>
    `).join("");

    body.querySelectorAll("input[data-idx]").forEach(inp=>{
      inp.addEventListener("input", ()=>{
        const i = Number(inp.getAttribute("data-idx"));
        chosen[i].ratio = Number(inp.value||0);
        setBadge("#ratioSum", `${ratioSum()}%`);
      });
    });
    body.querySelectorAll("button[data-del]").forEach(btn=>{
      btn.addEventListener("click", ()=>{
        const i = Number(btn.getAttribute("data-del"));
        chosen.splice(i,1);
        renderChosen();
      });
    });

    setBadge("#ratioSum", `${ratioSum()}%`);
  }

  $("#goStep2")?.addEventListener("click", async ()=>{
    if(!chosen.length){ alert("상품을 하나 이상 선택하세요."); return; }
    if(ratioSum() !== 100){ alert("운용 비율 합계가 100%가 되어야 합니다."); return; }

    try{
      // 현금성을 제외한 상품들에 대해서만 서류 동의
      const items = chosen.filter(c=>c.productType!=="CASH").map(c=>({ productType:c.productType, productId:c.productId }));
      const res = await fetch("/purchase/documents", {
        method: "POST",
        headers: { "Content-Type":"application/json" },
        body: JSON.stringify({ items })
      });
      if(!res.ok) throw new Error(`POST /purchase/documents → ${res.status}`);
      const docs = await res.json();
      state.docs = Array.isArray(docs) ? docs : (docs.data||[]);

      const box = $("#docBox");
      if(!box) return;

      if(!state.docs.length){
        box.innerHTML = "동의할 서류가 없습니다.";
      }else{
        box.innerHTML = state.docs.map(d=>`
          <div class="row">
            <label>
              <input type="checkbox" data-doc="${d.docId||d.fileUrl}"/> ${d.docType||"서류"}
            </label>
            <a class="btn ghost" href="${d.fileUrl||'#'}" target="_blank">열기</a>
          </div>
        `).join("");
      }
      // 다음 단계로 스크롤
      window.scrollTo({top: document.body.scrollHeight, behavior:"smooth"});
    }catch(e){
      console.error(e);
      alert("서류 목록 로드 중 오류가 발생했습니다.");
    }
  });

// ────────────────────────────────────────────────────────────────
// STEP 2 → STEP 3 — 서류 동의 확인 후 미리보기
// ────────────────────────────────────────────────────────────────
  $("#goStep3")?.addEventListener("click", async ()=>{
    // 모든 서류 동의 체크 확인(현금성 제외)
    const checks = Array.from(document.querySelectorAll("#docBox input[type='checkbox']"));
    const allAgree = checks.length === 0 || checks.every(ch => ch.checked);
    if(!allAgree){ alert("모든 서류에 동의해 주세요."); return; }

    const userId     = $("#userId")?.value?.trim();
    const accountType= $("#accountType")?.value || "IRP";
    const acountId   = $("#acountId")?.value?.trim();

    if(!userId || !acountId){
      alert("userId / acountId 를 입력하세요.");
      return;
    }
    if(ratioSum() !== 100){ alert("운용 비율 합계가 100%가 되어야 합니다."); return; }

    try{
      const payload = { userId, flow, accountType, acountId, items: chosen };
      const res = await fetch("/purchase/preview", {
        method: "POST",
        headers: { "Content-Type":"application/json" },
        body: JSON.stringify(payload)
      });
      if(!res.ok) throw new Error(`POST /purchase/preview → ${res.status}`);
      const json = await res.json();

      $("#beforeBox").textContent = JSON.stringify(json.before||{}, null, 2);
      $("#afterBox").textContent  = JSON.stringify(json.after ||{}, null, 2);

      window.scrollTo({top: document.body.scrollHeight, behavior:"smooth"});
    }catch(e){
      console.error(e);
      alert("미리보기 중 오류가 발생했습니다.");
    }
  });

// ────────────────────────────────────────────────────────────────
// STEP 3 — 적용(비밀번호 확인 → 즉시 반영)
// ────────────────────────────────────────────────────────────────
  $("#applyBtn")?.addEventListener("click", async ()=>{
    const userId     = $("#userId")?.value?.trim();
    const accountType= $("#accountType")?.value || "IRP";
    const acountId   = $("#acountId")?.value?.trim();
    const acctPwd    = $("#acctPwd")?.value;

    if(!userId || !acountId || !acctPwd){
      alert("userId / acountId / 계좌 비밀번호를 확인하세요.");
      return;
    }
    if(ratioSum() !== 100){ alert("운용 비율 합계가 100%가 되어야 합니다."); return; }

    setBadge("#applyStatus", "적용중…");
    try{
      const payload = { userId, flow, accountType, acountId, items: chosen, acctPwd };
      const res = await fetch("/purchase/apply", {
        method: "POST",
        headers: { "Content-Type":"application/json" },
        body: JSON.stringify(payload)
      });
      if(!res.ok) throw new Error(`POST /purchase/apply → ${res.status}`);
      const json = await res.json();

      const done = $("#doneBox");
      if(done) done.textContent = JSON.stringify(json, null, 2);

      setBadge("#applyStatus", "완료");

      // 완료 섹션으로 스크롤
      window.scrollTo({top: document.body.scrollHeight, behavior:"smooth"});
    }catch(e){
      console.error(e);
      setBadge("#applyStatus", "오류");
      alert("적용 중 오류가 발생했습니다.");
    }
  });

})();
