package com.ideafly.utils;

import com.ideafly.dto.auth.TelegramAuthDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Telegram登录验证工具类
 */
@Slf4j
public class TelegramAuthUtil {

    /**
     * 验证Telegram登录数据
     * @param telegramAuthDto Telegram认证数据
     * @param botToken Telegram Bot令牌
     * @return 是否验证通过
     */
    public static boolean verify(TelegramAuthDto telegramAuthDto, String botToken) {
        try {
            
            // 1. 检查必要字段是否存在
            if (!StringUtils.hasText(telegramAuthDto.getId()) || 
                !StringUtils.hasText(telegramAuthDto.getFirstName()) || 
                telegramAuthDto.getAuthDate() == null ||
                !StringUtils.hasText(telegramAuthDto.getHash())) {
                log.error("Telegram数据缺少必要字段");
                return false;
            }
            
            // 2. 检查auth_date是否过期（24小时有效）
            long authDate = telegramAuthDto.getAuthDate();
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime - authDate > 86400) {
                log.error("Telegram认证数据已过期: authDate={}, currentTime={}", authDate, currentTime);
                return false;
            }
            
            // 3. 创建数据字符串，排除hash字段
            Map<String, String> dataToCheck = new TreeMap<>();
            dataToCheck.put("auth_date", String.valueOf(telegramAuthDto.getAuthDate()));
            dataToCheck.put("first_name", telegramAuthDto.getFirstName());
            dataToCheck.put("id", telegramAuthDto.getId());
            
            // 添加可选字段
            if (StringUtils.hasText(telegramAuthDto.getUsername())) {
                dataToCheck.put("username", telegramAuthDto.getUsername());
            }
            if (StringUtils.hasText(telegramAuthDto.getPhotoUrl())) {
                dataToCheck.put("photo_url", telegramAuthDto.getPhotoUrl());
            }
            
            // 构建data_check_string - 与前端保持完全一致的格式
            StringBuilder dataCheckString = new StringBuilder();
            for (Map.Entry<String, String> entry : dataToCheck.entrySet()) {
                if (dataCheckString.length() > 0) {
                    dataCheckString.append('\n');
                }
                dataCheckString.append(entry.getKey()).append('=').append(entry.getValue());
            }
            
            log.debug("验证数据字符串: {}", dataCheckString);
            
            // 4. 计算密钥 - 使用SHA256对botToken进行哈希
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] secretKeyBytes = sha256.digest(botToken.getBytes(StandardCharsets.UTF_8));
            
            // 5. 计算HMAC-SHA256哈希
            HmacUtils hmacSha256 = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKeyBytes);
            byte[] hmacSha256Bytes = hmacSha256.hmac(dataCheckString.toString().getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制
            StringBuilder calculatedHashHex = new StringBuilder();
            for (byte b : hmacSha256Bytes) {
                calculatedHashHex.append(String.format("%02x", b));
            }
            
            log.debug("计算得到的哈希: {}", calculatedHashHex);
            log.debug("收到的哈希: {}", telegramAuthDto.getHash());
            
            // 6. 比较计算的哈希值与收到的哈希值
            boolean result = calculatedHashHex.toString().equals(telegramAuthDto.getHash());
            if (!result) {
                log.error("Telegram哈希验证失败 - 计算的哈希: {}, 收到的哈希: {}", calculatedHashHex, telegramAuthDto.getHash());
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            return false;
        } catch (Exception e) {
            log.error("验证Telegram数据时发生错误", e);
            return false;
        }
    }
} 