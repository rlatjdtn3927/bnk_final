/* -------------------------------------------------------------------
 * products.js — 상품관리 탭
 *
 * 역할
 * 1) 보유상품 목록 조회  : GET  /purchase/api/portfolio
 * 2) 변경 미리보기      : POST /purchase/api/portfolio/change/preview
 * 3) 변경 즉시 적용     : POST /purchase/api/portfolio/change/apply
 *
 * 입력 형식(텍스트에어리어 #changeLines):
 *  - 권장: ratio 기반 (합이 100%)
 *    [
 *      {"productType":"FUND","productId":"F001","ratio":80},
 *      {"productType":"PRINCIPAL","productId":"D001","ratio":20}
 *    ]
 *  - 대응: quantity 기반 (합계 → ratio로 환산; 정확도는 낮음, 미리보기 용도)
 *    [
 *      {"productType":"FUND","productId":"F001","action":"BUY","quantity":"1.00"},
 *      {"productType":"PRINCIPAL","productId":"D001","action":"BUY","quantity":"0.25"}
 *    ]
 *
 * 주의: CASH(현금성)는 상품이 아니므로 ratio에 포함되더라도 “잔액 유지”로만 동작(매수/저장 X).
 * ------------------------------------------------------------------ */

(function(){
  const $  = (s, el=document)=> el.querySelector(s);
  const $$ = (s, el=document)=> Array.from(el.querySelectorAll(s));

  const post = async (url, data) => {
    const res = await fetch(url,{
      method:"POST",
      headers:{ "Content-Type":"application/json" },
      body: JSON.stringify(data)
    });
    if(!res.ok) throw new Error(`POST ${url} -> ${res.status}`);
    return res.json();
  };
  const get = async (url) => {
    const res = await fetch(url);
    if(!res.ok) throw new Error(`GET ${url} -> ${res.status}`);
    return res.json();
  };

  const setStatus = (text)=> { const el=$("#statusBadge"); if(el) el.textContent=text; };
  const fmtNum = (v)=> {
    if(v===null || v===undefined) return "-";
    const n = Number(v);
    return isNaN(n) ? String(v) : n.toLocaleString();
  };

  /* ---------------------------------------------------------------
   * 0) 공통: 계정/계좌 읽기
   * ------------------------------------------------------------- */
  function readAccount(){
    const accountType = $("#accountType")?.value || "IRP";
    const acountId    = $("#acountId")?.value?.trim();
    if(!acountId) throw new Error("acountId를 입력하세요.");
    return { accountType, acountId };
  }

  /* ---------------------------------------------------------------
   * 1) 보유상품 목록 조회
   * ------------------------------------------------------------- */
  async function loadPortfolio(){
    try{
      setStatus("조회중…");
      const { accountType, acountId } = readAccount();
      const qs = new URLSearchParams({ accountType, acountId }).toString();
      const rows = await get(`/purchase/api/portfolio?${qs}`);

      const list = Array.isArray(rows) ? rows : (rows.data || []);
      const body = $("#portfolioBody");
      if(!body) return;

      if(!list.length){
        body.innerHTML = `<tr><td colspan="4">데이터 없음</td></tr>`;
      }else{
        body.innerHTML = list.map(r => `
          <tr>
            <td>${r.productId ?? "-"}</td>
            <td>${r.productType ?? "-"}</td>
            <td class="right">${r.quantity ?? "-"}</td>
            <td class="right">${fmtNum(r.evalAmt)}</td>
          </tr>
        `).join("");
      }
      setStatus("완료");
    }catch(e){
      console.error(e);
      setStatus("오류");
      alert(e.message || "보유상품 조회 중 오류");
    }
  }

  /* ---------------------------------------------------------------
   * 2) 텍스트에어리어 → items 파싱
   *   - ratio 형식: 그대로 사용(합 100 검증)
   *   - quantity 형식: 합계 기준으로 임시 ratio 산출(정확도 낮음)
   * ------------------------------------------------------------- */
  function parseChangeLines(){
    const raw = $("#changeLines")?.value?.trim();
    if(!raw) throw new Error("변경 입력이 비어 있습니다.");
    let arr;
    try { arr = JSON.parse(raw); }
    catch(e){ throw new Error("JSON 파싱 실패: 올바른 배열 형식으로 입력하세요."); }
    if(!Array.isArray(arr) || !arr.length) throw new Error("배열 형태로 한 개 이상 입력하세요.");

    // 2-1) ratio 기반이면 그대로 정규화
    if(arr.every(it => typeof it.ratio === "number")){
      const total = arr.reduce((a,c)=> a + (Number(c.ratio)||0), 0);
      if(total !== 100) throw new Error(`운용비율 합계가 ${total}% 입니다. 100%가 되어야 합니다.`);
      // 최소 필드 보정
      return arr.map(it => ({
        productType: String(it.productType || "").toUpperCase(),
        productId  : it.productId ?? null,
        ratio      : Number(it.ratio)||0
      }));
    }

    // 2-2) quantity 기반이면 합계→ratio 추정 (미리보기/시연용)
    if(arr.every(it => it.quantity !== undefined)){
      const nums = arr.map(it => Number(it.quantity)||0);
      const sum  = nums.reduce((a,c)=>a+c,0);
      if(sum <= 0) throw new Error("quantity 합계가 0 입니다.");
      // 비율 환산(정수 퍼센트, 오차는 마지막 항목에 흡수)
      let rest = 100;
      const items = arr.map((it, idx) => {
        const base = Math.floor((Number(it.quantity)||0) * 100 / sum);
        const ratio = (idx === arr.length-1) ? rest : base;
        rest -= base;
        return {
          productType: String(it.productType || "").toUpperCase(),
          productId  : it.productId ?? null,
          ratio
        };
      });
      return items;
    }

    throw new Error("지원하지 않는 형식입니다. ratio 또는 quantity 중 하나를 사용하세요.");
  }

  /* ---------------------------------------------------------------
   * 3) 변경 미리보기
   *   - AP: PORTFOLIO_CHANGE_PREVIEW
   *   - payload: { accountType, acountId, items:[{productType,productId,ratio}] }
   * ------------------------------------------------------------- */
  async function preview(){
    try{
      setStatus("미리보기중…");
      const { accountType, acountId } = readAccount();
      const items = parseChangeLines();

      const res = await post("/purchase/api/portfolio/change/preview", {
        accountType, acountId, items
      });

      $("#changeResult").textContent = JSON.stringify(res, null, 2);
      setStatus("완료");
    }catch(e){
      console.error(e);
      setStatus("오류");
      alert(e.message || "미리보기 중 오류");
    }
  }

  /* ---------------------------------------------------------------
   * 4) 변경 적용(즉시)
   *   - AP: PORTFOLIO_CHANGE_APPLY
   *   - payload: + acctPwd (IRP인 경우 비번 체크)
   * ------------------------------------------------------------- */
  async function apply(){
    try{
      setStatus("적용중…");
      const { accountType, acountId } = readAccount();
      const items = parseChangeLines();

      let acctPwd = "";
      if(accountType === "IRP"){ // IRP만 계좌 비밀번호 확인
        acctPwd = window.prompt("IRP 계좌 비밀번호를 입력하세요.", "") || "";
        if(!acctPwd) { setStatus("대기"); return; }
      }

      const res = await post("/purchase/api/portfolio/change/apply", {
        accountType, acountId, items, acctPwd
      });

      $("#changeResult").textContent = JSON.stringify(res, null, 2);
      setStatus("완료");

      // 적용 후 보유목록 갱신
      await loadPortfolio();
    }catch(e){
      console.error(e);
      setStatus("오류");
      alert(e.message || "적용 중 오류");
    }
  }

  /* ---------------------------------------------------------------
   * 5) 이벤트 바인딩 & 초기 로드
   * ------------------------------------------------------------- */
  $("#loadPortfolio")?.addEventListener("click", loadPortfolio);
  $("#previewBtn")?.addEventListener("click", preview);
  $("#applyBtn")?.addEventListener("click", apply);

  // Enter로 계좌 입력 후 바로 조회
  $("#acountId")?.addEventListener("keydown", (e)=>{
    if(e.key === "Enter") loadPortfolio();
  });

})();
