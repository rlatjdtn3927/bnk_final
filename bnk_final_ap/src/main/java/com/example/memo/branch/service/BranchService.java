package com.example.memo.branch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

	//외부 HTTP 호출용
    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${kakao.rest-key}")
    private String kakaoRestKey;

    private static final String KAKAO_LOCAL_SEARCH =
            "https://dapi.kakao.com/v2/local/search/keyword.json";

    //입력 내 좌표(위도/경도/검색 반경)
    public Map<String, Object> findNearbyBusanBank(double lat, double lon, double radiusKmInput) {

        // 카카오 반경은 미터, 최대 20,000m
        int radiusM = (int) Math.round(Math.max(0.5, radiusKmInput) * 1000);
        radiusM = Math.min(radiusM, 20000);
        //15개로 여유롭게 받아와서 나중에 5개로 추림
        List<Map<String, Object>> docs = searchKakao(lat, lon, radiusM, 15);

        //카카오가 주는 category_group_code가 BK9(은행) 인 것만 남김.
        //이름에 “부산은행” 이 포함된 것만.
        List<Map<String, Object>> filtered = docs.stream()
                .filter(m -> "BK9".equals(m.get("category_group_code")))
                .filter(m -> String.valueOf(m.get("place_name")).contains("부산은행"))
                .sorted(Comparator.comparingInt(m -> Integer.parseInt(String.valueOf(m.getOrDefault("distance", "999999")))))
                .limit(5)
                .collect(Collectors.toList());

        List<Map<String, Object>> branches = new ArrayList<>();
        for (Map<String, Object> d : filtered) {
            double by = Double.parseDouble(String.valueOf(d.get("y"))); // 위도
            double bx = Double.parseDouble(String.valueOf(d.get("x"))); // 경도
            int distM = Integer.parseInt(String.valueOf(d.getOrDefault("distance", "0")));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orgCode", "032");
            item.put("branchCode", d.get("id")); // 카카오 place id
            item.put("branchName", d.get("place_name"));
            item.put("latitude", by);
            item.put("longitude", bx);
            item.put("closeScheduled", false);
            item.put("closeDate", "00000000");
            item.put("distanceKm", Math.round((distM / 1000.0) * 1000) / 1000.0);
            branches.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", branches.size());
        result.put("branches", branches);
        return result;
    }

    //카카오 호출
    private List<Map<String, Object>> searchKakao(double lat, double lon, int radiusM, int size) {
    	//부산은행, 내 좌표(x=lon,y=lat), 반경(m), 최대 size 15, 거리순 정렬.
    	URI uri = UriComponentsBuilder.fromHttpUrl(KAKAO_LOCAL_SEARCH)
    		    .queryParam("query", "부산은행")
    		    .queryParam("y", lat)
    		    .queryParam("x", lon)
    		    .queryParam("radius", radiusM)
    		    .queryParam("size", Math.min(size, 15))
    		    .queryParam("sort", "distance")
    		    .build(false)                       // 아직 인코딩 안 됨
    		    .encode(StandardCharsets.UTF_8)     // UTF-8로 인코딩
    		    .toUri();

    	//카카오 키 헤더에 담기
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        //호출 및 상태코드 체크
        ResponseEntity<JsonNode> resp =
                http.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Kakao Local error: " + resp.getStatusCode());
        }

        //documents 배열을 가벼운 Map 리스트로 변환해 반환.
        JsonNode docs = resp.getBody().path("documents");
        List<Map<String, Object>> out = new ArrayList<>();
        if (docs.isArray()) {
            for (JsonNode n : docs) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", n.path("id").asText());
                m.put("place_name", n.path("place_name").asText());
                m.put("category_group_code", n.path("category_group_code").asText());
                m.put("x", n.path("x").asText()); // 경도
                m.put("y", n.path("y").asText()); // 위도
                m.put("distance", n.path("distance").asText()); // 미터 (sort=distance에서 제공)
                m.put("place_url", n.path("place_url").asText());
                m.put("road_address_name", n.path("road_address_name").asText());
                m.put("address_name", n.path("address_name").asText());
                out.add(m);
            }
        }
        return out;
    }
}
