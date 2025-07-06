package com.ideafly.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecretKeyGenerator {
    public static void main(String[] args) {
        try {
            // 创建 KeyGenerator 对象
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256); // 初始化 KeyGenerator，设置密钥长度为 256 位

            // 生成密钥
            SecretKey secretKey = keyGen.generateKey();
            byte[] keyBytes = secretKey.getEncoded();

            // 将密钥编码为 Base64 字符串
            String base64Key = Base64.getEncoder().encodeToString(keyBytes);

            System.out.println("Generated Key: " + base64Key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
