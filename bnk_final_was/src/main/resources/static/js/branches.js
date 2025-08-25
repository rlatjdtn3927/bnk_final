(function () {
  'use strict';

  const $ = s => document.querySelector(s);
  const btn = $("#btn"), btxt = $("#btxt"), bsp = $("#bsp");
  const mypos = $("#mypos"), list = $("#list"), err = $("#err"), empty = $("#empty");
  const radiusInput = $("#radius"), resultsSection = $("#results");

  let userLat = null, userLon = null;

/* ----------- 공통 유틸 ----------- */
function busy(on) {
  btn.disabled = on;
  btxt.textContent = on ? "찾는 중..." : "내 주변 지점 찾기";
  bsp.style.display = on ? "inline-block" : "none";
}
const fmt = (n, p = 6) => Number(n).toFixed(p);
const km = n => Number(n).toFixed(1); // [변경] 소수점 1자리로 통일

/* ----------- 한국식 주소 포맷 ----------- */
/**
 * 목표 포맷:
 *   "부산광역시 수영구 남천동 수영로 12-3 (48271)"
 * 룰:
 * 1) 시/도 (state/region/province)
 * 2) 시·군·구 (city_district/district/county/borough/city)
 * 3) 동/리 (suburb/neighbourhood/quarter/village/town/hamlet)
 * 4) 도로명 + 번지(있으면)
 * 5) (우편번호) - 있으면 괄호 포함
 */
function formatKrAddress(addr) {
  if (!addr) return null;

  // 시/도
  const sido =
    addr.state || addr.region || addr.province || addr.state_district;

  // 시·군·구
  const sigungu =
    addr.city_district || addr.district || addr.county || addr.borough || addr.city;

  // 동/리(지번 보조)
  const dongri =
    addr.suburb || addr.neighbourhood || addr.quarter || addr.village || addr.town || addr.hamlet;

  // 도로명 + 번지
  const road  = addr.road;
  const house = addr.house_number;

  // 우편번호
  const postcode = addr.postcode;

  const parts = [];
  if (sido) parts.push(sido);
  if (sigungu) parts.push(sigungu);
  if (dongri) parts.push(dongri);

  if (road) {
    parts.push(house ? `${road} ${house}` : road);
  }

  let line = parts.join(' ').trim();

  // 도로명 정보가 전혀 없고 동/리까지만 있으면 그대로 사용
  // 둘 다 없으면 display_name로 폴백(호출부에서 처리)
  if (postcode) line += ` (${postcode})`;

  return line || null;
}

/* ----------- 위치/역지오코딩 ----------- */
async function geolocate() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) return reject(new Error("이 브라우저는 위치 기능을 지원하지 않습니다."));
    navigator.geolocation.getCurrentPosition(
      pos => resolve({lat:pos.coords.latitude, lon:pos.coords.longitude}),
      err => reject(err),
      { enableHighAccuracy:true, timeout:12000, maximumAge:0 }
    );
  });
}

async function reverseGeocode(lat, lon) {
  try {
    const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lon)}&accept-language=ko`;
    const res = await fetch(url, { headers: { "Accept":"application/json" }});
    if (!res.ok) throw new Error("reverse geocode http " + res.status);
    const data = await res.json();

    // 먼저 한국식 포맷 시도 → 실패 시 display_name으로 폴백
    const pretty = formatKrAddress(data.address);
    return pretty || data.display_name || null;
  } catch(e){ return null; }
}

/* ----------- 초기화/렌더/호출 ----------- */
 async function init() {
    try {
      const p = await geolocate();
      userLat = p.lat; userLon = p.lon;
      const addr = await reverseGeocode(userLat, userLon);
      mypos.textContent = addr ? addr : `위도 ${fmt(userLat, 4)}, 경도 ${fmt(userLon, 4)}`;
    } catch (e) {
      mypos.textContent = "위치 권한을 허용해주세요.";
      // 권한 거부 시 에러 메시지 표시
      if (e.code === 1) {
        err.textContent = "지점을 찾으려면 위치 정보 접근 권한이 필요합니다.";
        err.style.display = "block";
        resultsSection.style.display = "block";
      }
    }
  }

  // [변경] 새로운 CSS 디자인에 맞게 렌더링 함수 전체 수정
  function render(items) {
    list.innerHTML = "";
    empty.style.display = items.length ? "none" : "block";
    if (!items.length) return;

    const frag = document.createDocumentFragment();
    items.forEach(b => {
      // 카카오맵 길찾기 링크 생성
      const navLink = `https://map.kakao.com/link/to/${b.branchName},${b.latitude},${b.longitude}`;
      const telLink = b.tel ? `tel:${b.tel}` : null;

      const wrap = document.createElement("div");
      wrap.className = "list-item";
      // 새로운 UI 구조에 맞게 데이터 삽입
      wrap.innerHTML = `
        <div class="info">
          <h3 class="branch-name">${b.branchName || "-"}</h3>
          <p class="address">${b.address || `지점코드: ${b.branchCode || "-"}`}</p>
        </div>
        <div class="distance">${km(b.distanceKm)}km</div>
        <div class="actions">
          <a href="${navLink}" target="_blank" class="action-btn">🗺️ 길찾기</a>
          ${telLink ? `<a href="${telLink}" class="action-btn">📞 전화</a>` : ''}
        </div>`;
      frag.appendChild(wrap);
    });
    list.appendChild(frag);
  }

  async function fetchNearby() {
    err.style.display = "none";
    err.textContent = "";
    resultsSection.style.display = "block"; // [추가] 결과 섹션 보이기

    try {
      busy(true);
      if (userLat == null || userLon == null) {
        await init(); // 위치 정보가 없으면 다시 가져오기
        if (userLat == null) throw new Error("위치 정보를 가져올 수 없습니다.");
      }

      const radiusKm = Number(radiusInput.value || "2.0");
      const res = await fetch("/branches/nearby", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ latitude: userLat, longitude: userLon, radiusKm })
      });

      if (!res.ok) throw new Error("서버 오류: " + res.status);
      const data = await res.json();
      if (data.error) throw new Error(data.error);

      // API 응답 데이터에 tel, address 필드가 있다고 가정
      render(data.branches || []);

    } catch (e) {
      err.textContent = e.message || String(e);
      err.style.display = "block";
      list.innerHTML = "";
      empty.style.display = "none";
    } finally {
      busy(false);
    }
  }

  init();
  btn.addEventListener("click", fetchNearby);

})();