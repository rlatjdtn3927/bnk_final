package com.example.memo.company.service;

import org.springframework.stereotype.Service;

//AP 서버
@Service
public class CryptoService {
 // 실제로는 강력한 암호화 알고리즘(AES-256 등)을 사용해야 합니다.
 public String encrypt(String plainText) {
     // 예시: Base64 인코딩 (실제 암호화가 아님)
     if (plainText == null) return null;
     return java.util.Base64.getEncoder().encodeToString(plainText.getBytes());
 }

 public String decrypt(String encryptedText) {
     if (encryptedText == null) return null;
     return new String(java.util.Base64.getDecoder().decode(encryptedText));
 }
}
