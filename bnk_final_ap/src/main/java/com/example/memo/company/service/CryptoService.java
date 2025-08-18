package com.example.memo.company.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class CryptoService {

    // application.yml 파일에서 주입받을 32바이트(32자리) 비밀 키
    @Value("${crypto.key}")
    private String key;

    // application.yml 파일에서 주입받을 16바이트(16자리) 초기화 벡터(IV)
    @Value("${crypto.iv}")
    private String iv;

    // 사용할 암호화 알고리즘 및 패딩 방식
    private final String transformation = "AES/CBC/PKCS5Padding";

    /**
     * 데이터를 AES-256으로 암호화
     * @param plainText 암호화할 평문 문자열
     * @return Base64로 인코딩된 암호문 문자열
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AES-256으로 암호화된 데이터를 복호화
     * @param encryptedText Base64로 인코딩된 암호문 문자열
     * @return 복호화된 평문 문자열
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("복호화 중 오류가 발생했습니다.", e);
        }
    }
}