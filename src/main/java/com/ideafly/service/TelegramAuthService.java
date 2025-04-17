package com.ideafly.service;

import com.ideafly.dto.TelegramAuthDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class TelegramAuthService {
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    public boolean verifyTelegramAuth(TelegramAuthDto authData) {
        // 检查认证是否过期
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime - authData.getAuthDate() > 86400) {
            log.warn("Telegram验证已过期：{}", authData.getAuthDate());
            return false;
        }
        
        // 验证数据完整性
        String hash = authData.getHash();
        
        try {
            // 创建检查字符串
            Map<String, String> dataToCheck = new TreeMap<>();
            dataToCheck.put("id", authData.getId());
            dataToCheck.put("first_name", authData.getFirstName());
            if (authData.getLastName() != null) {
                dataToCheck.put("last_name", authData.getLastName());
            }
            if (authData.getUsername() != null) {
                dataToCheck.put("username", authData.getUsername());
            }
            if (authData.getPhotoUrl() != null) {
                dataToCheck.put("photo_url", authData.getPhotoUrl());
            }
            dataToCheck.put("auth_date", String.valueOf(authData.getAuthDate()));
            
            StringBuilder checkStr = new StringBuilder();
            for (Map.Entry<String, String> entry : dataToCheck.entrySet()) {
                if (checkStr.length() > 0) {
                    checkStr.append("\n");
                }
                checkStr.append(entry.getKey()).append("=").append(entry.getValue());
            }
            
            // 计算密钥
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));
            
            // 计算HMAC-SHA256
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            hmac.init(keySpec);
            byte[] hashBytes = hmac.doFinal(checkStr.toString().getBytes(StandardCharsets.UTF_8));
            
            // 验证哈希
            String calculatedHash = bytesToHex(hashBytes);
            boolean isValid = calculatedHash.equals(hash);
            
            if (!isValid) {
                log.warn("Telegram验证哈希不匹配. 计算结果: {}, 提供值: {}", calculatedHash, hash);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("验证Telegram认证失败", e);
            return false;
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
} 