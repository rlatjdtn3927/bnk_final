const $ = s => document.querySelector(s);
const btn=$("#btn"), btxt=$("#btxt"), bsp=$("#bsp");
const mypos=$("#mypos"), list=$("#list"), err=$("#err"), empty=$("#empty");

let userLat=null, userLon=null;

/* ----------- 공통 유틸 ----------- */
function busy(on){
  btn.disabled=on; btxt.textContent=on?"조회 중…":"5개 지점 조회"; bsp.style.display=on?"inline-block":"none";
}
const fmt=(n, p=6)=>Number(n).toFixed(p);
const km = n => Number(n).toFixed(3);

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
async function init(){
  try{
    const p = await geolocate();
    userLat=p.lat; userLon=p.lon;
    const addr = await reverseGeocode(userLat, userLon);
    mypos.textContent = addr ? addr : `${fmt(userLat)}, ${fmt(userLon)}`;
  }catch(e){
    mypos.textContent = "위치 권한이 필요합니다.";
  }
}

function render(items){
  list.innerHTML="";
  empty.style.display = items.length ? "none":"block";
  if (!items.length) return;

  const frag=document.createDocumentFragment();
  items.forEach((b,i)=>{
    const gmaps = `https://www.google.com/maps/search/?api=1&query=${b.latitude},${b.longitude}`;
    const nav = `https://maps.google.com/?daddr=${b.latitude},${b.longitude}`;
    const wrap=document.createElement("div");
    wrap.className="item";
    wrap.innerHTML = `
      <div class="rank">${i+1}</div>
      <div>
        <div class="name"><a href="${gmaps}" target="_blank" rel="noopener">${b.branchName||"-"}</a></div>
        <div class="addr">기관: 032 · 지점코드: ${b.branchCode||"-"}</div>
        <div class="meta"><span class="dist">${km(b.distanceKm)} km</span>
          <span class="muted">${fmt(b.latitude,5)}, ${fmt(b.longitude,5)}</span>
        </div>
      </div>
      <div class="actions">
        <a class="link" href="${gmaps}" target="_blank" rel="noopener">지도</a>
        <a class="link" href="${nav}" target="_blank" rel="noopener">길찾기</a>
      </div>`;
    frag.appendChild(wrap);
  });
  list.appendChild(frag);
}

async function fetchNearby(){
  err.style.display="none"; err.textContent="";
  try{
    busy(true);
    if (userLat==null || userLon==null) {
      const p = await geolocate(); userLat=p.lat; userLon=p.lon;
    }
    const radiusKm = Number($("#radius").value || "2.0");
    const res = await fetch("/branches/nearby", {
      method:"POST",
      headers:{ "Content-Type":"application/json" },
      body: JSON.stringify({ latitude:userLat, longitude:userLon, radiusKm })
    });
    if (!res.ok) throw new Error("서버 오류: " + res.status);
    const data = await res.json();
    if (data.error) throw new Error(data.error);
    render(data.branches || []);
  }catch(e){
    err.textContent = e.message || String(e);
    err.style.display="block";
  }finally{
    busy(false);
  }
}

init();
$("#btn").addEventListener("click", fetchNearby);
