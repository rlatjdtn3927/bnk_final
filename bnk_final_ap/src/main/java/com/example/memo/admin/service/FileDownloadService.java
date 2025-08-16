package com.example.memo.admin.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.springframework.stereotype.Service;

import com.example.memo.jpa.entity.purchase.analysis.FileTaskList;
import com.example.memo.jpa.repository.admin.FileTaskListRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final FileTaskListRepository fileTaskListRepository;
    private final ExecutorService downloadExecutor;
    private final HttpClient httpClient;

    public Map<String, Boolean> downloadFiles(JsonNode data) {
        List<String> prodIdList = new ObjectMapper().convertValue(
            data.get("prodIdList"), new TypeReference<List<String>>() {}
        );

        List<FileTaskList> taskList = fileTaskListRepository.findByProdIdIn(prodIdList);
        if (taskList == null || taskList.isEmpty()) return Map.of();

        // 결과/성공 목록 (스레드 안전)
        ConcurrentHashMap<String, Boolean> resultMap = new ConcurrentHashMap<>();
        List<FileTaskList> successTasks = Collections.synchronizedList(new ArrayList<>());

        // 비동기 태스크 생성
        List<CompletableFuture<Void>> futures = taskList.stream()
            .map(t -> {
                String prodId   = t.getProdId();
                String url      = t.getDownloadUrl();
                String category = t.getProdCategory();
                String fileName = t.getFileName();
                if (url == null || url.isBlank()) {
                    resultMap.put(prodId, false);
                    return CompletableFuture.<Void>completedFuture(null);
                }
                Path target = Paths.get("C:\\bnk_project").resolve(category).resolve(fileName);

                return downloadFileAsync(url, target) // 비동기 다운로드
                    .thenAccept(v -> {
                        resultMap.put(prodId, true);
                        successTasks.add(t);
                    })
                    .exceptionally(ex -> {
                        resultMap.put(prodId, false);
                        return null;
                    });
            })
            .toList();

        // 모두 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 성공한 태스크만 일괄 삭제 (DB는 메인 스레드에서 한 번만)
        if (!successTasks.isEmpty()) {
            fileTaskListRepository.deleteAllInBatch(successTasks);
        }
        return resultMap;
    }
    
    
    
    //파일 다운로드 유틸리티 함수
    private void downloadFile(String fileURL, String category, String fileName) throws IOException {
        Path base = Paths.get("C:\\bnk_project");
        Path targetPath = base.resolve(category).resolve(fileName);
        Files.createDirectories(targetPath.getParent());

        String encodedUrl = fileURL.replace(" ", "%20"); //띄어쓰기 인코딩
        URL url = new URL(encodedUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    private CompletableFuture<Void> downloadFileAsync(String fileURL, Path targetPath) {
        return CompletableFuture.runAsync(() -> {
            // 1) 디렉터리 준비
            try {
                Files.createDirectories(targetPath.getParent());
            } catch (IOException e) {
                throw new CompletionException(e);
            }

            // 2) URL 생성 (가능하면 기존 toSafeUri를 재사용해 인코딩 문제 회피)
            URL originalUrl = null;
			try {
				originalUrl = new URL(fileURL.replace(" ", "%20"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
      

            // 3) 임시 파일 경로(.part)
            Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".part");

            // 4) 리다이렉트(maxRedirects 회) + 2xx 상태코드 검증 + 파일로 스트리밍
            URL current = originalUrl;
            boolean downloaded = false;
            int maxRedirects = 5;

            try {
                for (int redirect = 0; redirect <= maxRedirects; redirect++) {
                    HttpURLConnection conn = (HttpURLConnection) current.openConnection();
                    conn.setInstanceFollowRedirects(false);     // 수동으로 처리 (Location 추적)
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10_000);             // 연결 타임아웃
                    conn.setReadTimeout(60_000);                // 읽기 타임아웃

                    int code = conn.getResponseCode();
                    if (code / 100 == 2) { // 2xx
                        try (InputStream in = conn.getInputStream()) {
                            Files.copy(in, tempPath,
                                StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                        downloaded = true;
                        break; // OK
                    } else if (code / 100 == 3) { // 3xx redirect
                        String location = conn.getHeaderField("Location");
                        conn.disconnect();
                        if (location == null || location.isBlank()) {
                            throw new IOException("Redirect without Location from " + current);
                        }
                        current = new URL(current, location); // 상대/절대 모두 처리
                        continue; // 다음 루프로 따라감
                    } else {
                        // 에러 스트림 소진 후 예외 (리소스 누수 방지)
                        try (InputStream err = conn.getErrorStream()) { /* no-op */ }
                        throw new IOException("HTTP " + code + " for " + current);
                    }
                }

                if (!downloaded) {
                    throw new IOException("Too many redirects for " + originalUrl);
                }

                // 5) 임시 → 본 파일로 원자적 이동(가능 시), 실패 시 폴백
                try {
                    Files.move(
                        tempPath,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    );
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException io) {
                // 실패 시 임시파일 정리 후 예외 전파
                try { Files.deleteIfExists(tempPath); } catch (IOException ignore) {}
                throw new CompletionException(io);
            }
        }, downloadExecutor); // 블로킹 다운로드를 전용 풀에서 비동기 실행
    }

}