// purchase-planned.js
(function () {
  function $(root, sel) { return root.querySelector(sel); }
  function show(el, on) { el && el.classList.toggle('hidden', !on); }
  function toast(root, msg) {
    const t = $('#toast', root);
    $('#toast-msg', root).textContent = msg;
    show(t, true); setTimeout(() => show(t, false), 1500);
  }

  async function apiGet(url) {
    const r = await fetch(url, { credentials: 'same-origin' });
    if (!r.ok) throw new Error(await r.text());
    return r.json();
  }
  async function apiPost(url, body) {
    const r = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify(body)
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) { const msg = data.error || r.statusText; throw new Error(msg); }
    return data;
  }

  // 전역 마운트 함수
  window.mountPlanned = function mountPlanned(contentEl) {
    const root = contentEl.querySelector('[data-mode]') || contentEl; // _planned root
    let mode = 'IRP';   // IRP | DC
    let pickerAll = false;
    let currentCat = '예금';
    const categories = ['예금', 'TDF', '펀드', 'ETF', '현금성자산'];

    const E = sel => $(root, sel);

    // 상태 로드 & 렌더
    async function loadState() {
      const d = await apiGet(`/api/purchase/planned/state?mode=${mode}`);
      renderList(d.items || []);
      E('#totalRatio').textContent = (d.totalRatio ?? 0) + '%';
      E('#cur-sum').textContent = d.totalRatio ?? 0;
    }

    function renderList(items) {
      const list = E('#item-list');
      const empty = E('#list-empty');
      list.innerHTML = '';
      show(empty, !items || items.length === 0);
      (items || []).forEach((it, idx) => {
        const row = document.createElement('div');
        row.className = 'border rounded-xl p-3';
        row.innerHTML = `
          <div class="flex items-start justify-between">
            <div>
              <div class="text-xs uppercase tracking-wider text-neutral-500">${it.category}</div>
              <div class="font-medium">${it.name}</div>
              <div class="text-sm text-neutral-500">${it.note || '-'}</div>
            </div>
            <div class="flex items-center gap-4">
              <div>
                <div class="text-xs text-neutral-500">비율(%)</div>
                <input type="number" min="0" max="100" class="w-24 border rounded px-2 py-1"
                       value="${it.ratio}" data-idx="${idx}" data-type="ratio">
              </div>
              <div>
                <div class="text-xs text-neutral-500">금액(원)</div>
                <input type="number" class="w-32 border rounded px-2 py-1"
                       value="${it.amount || 0}" data-idx="${idx}" data-type="amount">
              </div>
              <button class="px-3 py-1 border rounded-xl" data-idx="${idx}" data-type="remove">삭제</button>
            </div>
          </div>`;
        list.appendChild(row);
      });
    }

    // 모달
    async function openPicker(all) {
      pickerAll = !!all;
      show(E('#picker'), true);
      E('#picker-apply-all').classList.toggle('hidden', !pickerAll);
      buildCatButtons();
      await loadProducts(currentCat);
      E('#pick-ratio').value = '';
      E('#cur-sum').textContent = E('#totalRatio').textContent.replace('%', '');
    }
    function closePicker() { show(E('#picker'), false); }

    function buildCatButtons() {
      const wrap = E('#cat-buttons'); wrap.innerHTML = '';
      categories.forEach(c => {
        const b = document.createElement('button');
        b.className = 'px-3 py-1 rounded-xl border';
        b.textContent = c;
        if (c === currentCat) b.classList.add('bg-blue-600', 'text-white');
        b.onclick = async () => { currentCat = c; buildCatButtons(); await loadProducts(c); };
        wrap.appendChild(b);
      });
    }

    async function loadProducts(cat) {
      const data = await apiGet(`/api/purchase/planned/products?category=${encodeURIComponent(cat)}`);
      const list = E('#picker-list'); list.innerHTML = '';
      show(E('#picker-empty'), !(data.products && data.products.length));
      (data.products || []).forEach(p => {
        const row = document.createElement('div');
        row.className = 'p-3 flex items-center justify-between border-b';
        row.innerHTML = `
          <div>
            <div class="text-xs text-neutral-500">${p.category}</div>
            <div class="font-medium">${p.name}</div>
            <div class="text-sm text-neutral-500">${p.note || '-'}</div>
          </div>
          <button class="px-3 py-1 bg-blue-600 text-white rounded-xl">선택</button>`;
        row.querySelector('button').onclick = async () => {
          const ratio = +E('#pick-ratio').value || 0;
          if (ratio <= 0 || ratio > 100) return toast(root, '비율은 1~100');
          try {
            await apiPost('/api/purchase/planned/change', {
              mode, ratio, op: pickerAll ? 'replaceAll' : 'add', product: p
            });
            closePicker();
            await loadState();
          } catch (e) { toast(root, e.message); }
        };
        list.appendChild(row);
      });
    }

    // 이벤트 바인딩(위임)
    E('#btn-add')?.addEventListener('click', () => openPicker(false));
    E('#btn-replace-all')?.addEventListener('click', () => openPicker(true));
    E('#picker-close')?.addEventListener('click', closePicker);
    E('#picker-cancel')?.addEventListener('click', closePicker);

    E('#item-list')?.addEventListener('input', async (e) => {
      const t = e.target;
      const idx = +t.dataset.idx;
      if (Number.isNaN(idx)) return;
      try {
        if (t.dataset.type === 'ratio') {
          const data = await apiPost('/api/purchase/planned/change', { mode, op: 'updateRatio', index: idx, ratio: +t.value || 0 });
          E('#totalRatio').textContent = data.totalRatio + '%';
          E('#cur-sum').textContent = data.totalRatio;
        } else if (t.dataset.type === 'amount') {
          await apiPost('/api/purchase/planned/change', { mode, op: 'updateAmount', index: idx, amount: +t.value || 0 });
        }
      } catch (e2) { toast(root, e2.message); }
    });

    E('#item-list')?.addEventListener('click', async (e) => {
      const t = e.target;
      if (t.dataset.type === 'remove') {
        try {
          await apiPost('/api/purchase/planned/change', { mode, op: 'remove', index: +t.dataset.idx });
          await loadState();
        } catch (e2) { toast(root, e2.message); }
      }
    });

    // 단계 이동
    E('#btn-next')?.addEventListener('click', async () => {
      try {
        await apiPost('/api/purchase/planned/next', { mode });
        show(E('#agree-panel'), true);
        E('#btn-next').disabled = true;
      } catch (e) { toast(root, e.message); }
    });

    E('#btn-review')?.addEventListener('click', async () => {
      const agr = { mode,
        summary: $('#ag1', root).checked,
        terms: $('#ag2', root).checked,
        prospectus: $('#ag3', root).checked
      };
      try {
        await apiPost('/api/purchase/planned/agreements', agr);
        const d = await apiGet(`/api/purchase/planned/review?mode=${mode}`);
        const tb = E('#tbl-before'); tb.innerHTML = '';
        const ta = E('#tbl-after');  ta.innerHTML = '';
        (d.before || []).forEach(b => tb.insertAdjacentHTML('beforeend',
          `<tr class="border-b"><td class="py-2"><div class="text-xs text-neutral-500">${b.category}</div><div class="font-medium">${b.name}</div></td><td class="text-right">${b.ratio}</td></tr>`));
        (d.after || []).forEach(a => ta.insertAdjacentHTML('beforeend',
          `<tr class="border-b"><td class="py-2"><div class="text-xs text-neutral-500">${a.category}</div><div class="font-medium">${a.name}</div></td><td class="text-right">${a.ratio}</td></tr>`));
        E('#after-sum').textContent = d.totalAfter;
        show(E('#review-panel'), true);
      } catch (e) { toast(root, e.message); }
    });

    E('#btn-submit')?.addEventListener('click', async () => {
      try {
        await apiPost('/api/purchase/planned/submit', { mode });
        show(E('#done-panel'), true);
      } catch (e) { toast(root, e.message); }
    });

    E('#btn-done-ok')?.addEventListener('click', async () => {
      // IRP 끝나면 DC로 넘김(요구사항)
      mode = (mode === 'IRP') ? 'DC' : 'IRP';
      E('#mode-badge').textContent = (mode === 'IRP') ? '본인부담금(IRP)' : '퇴직금(DC)';
      E('#btn-toggle').textContent = (mode === 'IRP') ? '퇴직금으로 전환' : '본인부담금으로 전환';
      show(E('#agree-panel'), false); show(E('#review-panel'), false); show(E('#done-panel'), false);
      E('#btn-next').disabled = false;
      await loadState();
    });

    E('#btn-toggle')?.addEventListener('click', async () => {
      mode = (mode === 'IRP') ? 'DC' : 'IRP';
      E('#mode-badge').textContent = (mode === 'IRP') ? '본인부담금(IRP)' : '퇴직금(DC)';
      E('#btn-toggle').textContent = (mode === 'IRP') ? '퇴직금으로 전환' : '본인부담금으로 전환';
      show(E('#agree-panel'), false); show(E('#review-panel'), false); show(E('#done-panel'), false);
      E('#btn-next').disabled = false;
      await loadState();
    });

    // 초기 로드
    loadState();
  };
})();
