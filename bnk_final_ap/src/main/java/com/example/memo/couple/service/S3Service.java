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
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    /** 바이트 업로드 (AES256 서버사이드 암호화) */
    public String uploadBytes(byte[] data, String key, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            // 파일명 기반으로 추정(없으면 octet-stream)
            String guessed = URLConnection.guessContentTypeFromName(key);
            contentType = (guessed != null) ? guessed : "application/octet-stream";
        }

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.AES256) // KMS 필요 없음
                .build();

        s3Client.putObject(put, RequestBody.fromBytes(data));
        return key;
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

    /** 프리사인드 GET URL 생성 (예: 10분 유효) */
    public String generatePresignedGetUrl(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(ttl) // 예: Duration.ofMinutes(10)
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
        return presigned.url().toString();
    }
}
