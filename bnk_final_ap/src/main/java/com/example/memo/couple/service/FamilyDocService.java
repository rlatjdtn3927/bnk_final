package com.example.memo.couple.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyDocService {

    private final S3Service s3Service;

    public String uploadFile(Long linkId, String fileName, String contentType, byte[] fileData) {
        // 임시: OCR은 패스하고 그냥 tmp/에 올려보기
        String key = "couple-link/" + linkId + "/tmp/" + fileName;

        s3Service.uploadBytes(fileData, key, contentType);

        return key;
    }
}