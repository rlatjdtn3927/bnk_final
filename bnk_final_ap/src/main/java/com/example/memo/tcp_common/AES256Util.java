package com.example.memo.tcp_common;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AES256Util {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    // 256비트 키 (32바이트)
    @Value("${tcp.secretkey}")
    private static String SECRET_KEY;
    private static final String CHARSET = "UTF-8";

    public static String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        //랜덤 16바이트 난수 생성

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        //랜덤으로 생성된 16바이트 난수로 부터 IV 객체 생성
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(CHARSET), "AES");
        //비밀키 객체 생성 (SECRET_KEY를 사용하고 AES를 쓸것을 알림)

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        //암호화 복호화 당담 객체
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        //비밀키와 IV 객체를 통해, 암호화 모드로 초기화
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        //암호화 수행

        // IV와 암호문을 합쳐서 Base64 인코딩
        byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }

    public static String decrypt(String encryptedText) throws Exception {
        byte[] encryptedIvText = Base64.getDecoder().decode(encryptedText);

        byte[] iv = new byte[16];
        System.arraycopy(encryptedIvText, 0, iv, 0, iv.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        int encryptedSize = encryptedIvText.length - iv.length;
        byte[] encryptedBytes = new byte[encryptedSize];
        System.arraycopy(encryptedIvText, iv.length, encryptedBytes, 0, encryptedSize);

        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(CHARSET), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] decrypted = cipher.doFinal(encryptedBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}