package com.ideafly.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 游标分页工具类
 * 用于生成和解析基于时间戳和ID的复合游标
 */
public class CursorUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建游标字符串
     * 将时间戳和ID编码为base64字符串
     * 
     * @param timestamp 时间戳
     * @param id ID值
     * @return 编码后的游标字符串
     */
    public static String encodeCursor(Date timestamp, Integer id) {
        if (timestamp == null || id == null) {
            return null;
        }
        
        Map<String, Object> cursorMap = new HashMap<>();
        cursorMap.put("timestamp", timestamp);
        cursorMap.put("id", id);
        
        try {
            String json = objectMapper.writeValueAsString(cursorMap);
            String cursor = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            System.out.println("【CursorUtils】生成游标 - 时间戳: " + timestamp + ", ID: " + id + ", 游标: " + cursor);
            return cursor;
        } catch (JsonProcessingException e) {
            System.err.println("创建游标失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建游标字符串（LocalDateTime版本）
     * 将LocalDateTime转换为Date后进行编码
     * 
     * @param timestamp LocalDateTime时间戳
     * @param id ID值
     * @return 编码后的游标字符串
     */
    public static String encodeCursor(LocalDateTime timestamp, Integer id) {
        if (timestamp == null || id == null) {
            return null;
        }
        
        // 将LocalDateTime转换为Date
        Date date = Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
        return encodeCursor(date, id);
    }
    
    /**
     * 解析游标字符串
     * 将base64编码的游标解析为时间戳和ID
     * 
     * @param cursor 游标字符串
     * @return 包含timestamp和id的Map，解析失败返回null
     */
    public static Map<String, Object> decodeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        
        try {
            System.out.println("【CursorUtils.decodeCursor】开始解析游标: " + cursor);
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            System.out.println("【CursorUtils.decodeCursor】解码后的JSON: " + json);
            
            Map<String, Object> cursorMap = objectMapper.readValue(json, Map.class);
            
            // 确保游标包含必要的字段
            if (!cursorMap.containsKey("timestamp") || !cursorMap.containsKey("id")) {
                System.err.println("【CursorUtils】游标解析错误 - 缺少必要字段: " + json);
                return null;
            }
            
            // 处理Long转Date的问题
            Object timestampObj = cursorMap.get("timestamp");
            if (timestampObj instanceof Long) {
                cursorMap.put("timestamp", new Date((Long) timestampObj));
                System.out.println("【CursorUtils.decodeCursor】转换Long -> Date: " + cursorMap.get("timestamp"));
            } else if (timestampObj instanceof Integer) {
                cursorMap.put("timestamp", new Date(((Integer) timestampObj).longValue()));
                System.out.println("【CursorUtils.decodeCursor】转换Integer -> Date: " + cursorMap.get("timestamp"));
            } else if (timestampObj instanceof String) {
                // 尝试解析字符串格式的时间戳
                try {
                    Long timestamp = Long.parseLong((String) timestampObj);
                    cursorMap.put("timestamp", new Date(timestamp));
                    System.out.println("【CursorUtils.decodeCursor】转换String -> Date: " + cursorMap.get("timestamp"));
                } catch (NumberFormatException e) {
                    System.err.println("【CursorUtils】无法将字符串时间戳转换为Date: " + timestampObj);
                }
            }
            
            // 确保ID是整数
            Object idObj = cursorMap.get("id");
            if (!(idObj instanceof Integer)) {
                if (idObj instanceof Number) {
                    cursorMap.put("id", ((Number) idObj).intValue());
                    System.out.println("【CursorUtils.decodeCursor】转换Number -> Integer: " + cursorMap.get("id"));
                } else if (idObj instanceof String) {
                    try {
                        cursorMap.put("id", Integer.parseInt((String) idObj));
                        System.out.println("【CursorUtils.decodeCursor】转换String -> Integer: " + cursorMap.get("id"));
                    } catch (NumberFormatException e) {
                        System.err.println("【CursorUtils】无法将字符串ID转换为Integer: " + idObj);
                        return null;
                    }
                } else {
                    System.err.println("【CursorUtils】游标解析错误 - ID不是数字类型: " + idObj);
                    return null;
                }
            }
            
            System.out.println("【CursorUtils】解析游标成功 - 游标: " + cursor + 
                               ", 时间戳: " + cursorMap.get("timestamp") + 
                               ", ID: " + cursorMap.get("id"));
            
            return cursorMap;
        } catch (Exception e) {
            System.err.println("【CursorUtils】解析游标失败: " + cursor + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}