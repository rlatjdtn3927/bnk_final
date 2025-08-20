package com.example.memo.couple.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URLConnection;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;


/**
 * AWS SDK v2 기반 S3 유틸.
 * SpouseLinkService에서 호출하는 put(prefix, filename, bytes) 시그니처를 제공
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    /** 기존 코드 호환: prefix + 안전한 파일명으로 key 생성 후 업로드 */
    public String put(String prefix, String filename, byte[] bytes) {
        String safePrefix = normalizePrefix(prefix);
        String safeName   = safeFilename(filename);
        String key = safePrefix + UUID.randomUUID() + "_" + safeName;

        String contentType = guessContentType(safeName);
        uploadBytes(bytes, key, contentType);
        return key;
    }

    /** Base64 본문 업로드 편의 함수 */
    public String putBase64(String prefix, String filename, String base64) {
        byte[] data = Base64.getDecoder().decode(base64);
        return put(prefix, filename, data);
    }

    /** 바이트 업로드 (SSE-S3 적용) */
    public void uploadBytes(byte[] data, String key, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            contentType = guessContentType(key);
        }

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.AES256) // KMS 불필요, S3 관리형 키
                .build();

        s3Client.putObject(put, RequestBody.fromBytes(data));
    }

    /** 다운로드 */
    public byte[] download(String key) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.getObjectAsBytes(get).asByteArray();
    }

    /** 삭제 */
    public void delete(String key) {
        DeleteObjectRequest del = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(del);
    }

    /** 프리사인드 GET URL 생성 (기본 10분 권장) */
    public String presignedGetUrl(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(ttl == null ? Duration.ofMinutes(10) : ttl)
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
        return presigned.url().toString();
    }

    /* ================= helpers ================= */

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return "";
        String p = prefix.replace("\\", "/");
        if (!p.endsWith("/")) p = p + "/";
        if (p.startsWith("/")) p = p.substring(1);
        return p;
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        // 경로문자 제거 및 공백 정리
        String f = filename.replace("\\", "/");
        int idx = f.lastIndexOf('/');
        if (idx >= 0) f = f.substring(idx + 1);
        return f.replaceAll("[\\r\\n]", "").trim();
    }

    private String guessContentType(String nameOrKey) {
        String guessed = URLConnection.guessContentTypeFromName(nameOrKey);
        return (guessed != null) ? guessed : "application/octet-stream";
    }
}
