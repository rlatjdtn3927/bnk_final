/* ------------ 1) 임베딩 버튼 ------------ */
document.addEventListener('DOMContentLoaded', () => {
  const embedBtn = document.getElementById('embedBtn');
  const chatBox  = document.getElementById('chatBox');
  const form     = document.getElementById('chatForm');
  const input    = document.getElementById('question');

  embedBtn.addEventListener('click', () => {
    if (!confirm('static 폴더의 모든 PDF를 임베딩합니다.\n(진행 상황은 STS 콘솔에서 확인)')) return;

    fetch('/api/embeddings', {          // WAS → AP: EMBEDDING_CREATE
      method : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body   : '{}'                      // 빈 JSON
    })
    .then(r => r.text())
    .then(msg => alert('임베딩 완료: ' + msg))
    .catch(e => alert('임베딩 오류: ' + e));
  });

  /* ------------ 2) 채팅 UI ------------ */
  function appendBubble(role, text) {
    const div  = document.createElement('div');
    div.className = 'bubble ' + role;
    const span = document.createElement('span');
    span.textContent = text;
    div.appendChild(span);
    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
  }

  form.addEventListener('submit', e => {
    e.preventDefault();
    const message = input.value.trim();
    if (!message) return;

    appendBubble('user', message);
    input.value = '';
    appendBubble('assist', '...thinking');

    fetch('/api/chat', {                // WAS → AP: CHAT_ASK_QUESTION
      method : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body   : JSON.stringify({ question: message })
    })
    .then(r => r.text())
    .then(answer => {
      // 마지막 placeholder '...thinking' 교체
      chatBox.lastChild.remove();
      appendBubble('assist', answer);
    })
    .catch(e => {
      chatBox.lastChild.remove();
      appendBubble('assist', '❌ 오류: ' + e);
    });
  });
});
