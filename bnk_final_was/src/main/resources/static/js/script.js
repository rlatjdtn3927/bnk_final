// 사이드 내비 토글
const hamburger = document.querySelector('.hamburger-menu');
const overlay = document.querySelector('.overlay');
const side = document.querySelector('.side-nav');
const closeBtn = document.querySelector('.close-btn');

function openNav(){ side.classList.add('open'); overlay.classList.add('show'); }
function closeNav(){ side.classList.remove('open'); overlay.classList.remove('show'); }

hamburger?.addEventListener('click', openNav);
overlay?.addEventListener('click', closeNav);
closeBtn?.addEventListener('click', closeNav);

// Lucide SVG 아이콘 초기화
document.addEventListener('DOMContentLoaded', () => {
  if (window.lucide?.createIcons) window.lucide.createIcons();
});
