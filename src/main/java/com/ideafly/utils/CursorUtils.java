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
import java.text.SimpleDateFormat;

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
            return cursor;
        } catch (JsonProcessingException e) {
            // 静默处理或改为上层捕获
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
            // 开始解析游标
            
            // 检查是否是手动构建的老格式游标（ID:timestamp.SSS）
            if (cursor.contains(":") && !cursor.startsWith("ey")) {
                // 检测到手动构建的游标格式，尝试兼容处理
                // 尝试兼容处理
                String[] parts = cursor.split(":");
                if (parts.length == 2) {
                    try {
                        Integer id = Integer.parseInt(parts[0]);
                        // 将timestamp字符串转换为Date
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        Date timestamp = format.parse(parts[1]);
                        
                        Map<String, Object> cursorMap = new HashMap<>();
                        cursorMap.put("timestamp", timestamp);
                        cursorMap.put("id", id);
                        
                        // 兼容解析成功
                        return cursorMap;
                    } catch (Exception e) {
                        // 兼容解析失败
                    }
                }
            }
            
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            
            Map<String, Object> cursorMap = objectMapper.readValue(json, Map.class);
            
            // 确保游标包含必要的字段
            if (!cursorMap.containsKey("timestamp") || !cursorMap.containsKey("id")) {
                System.err.println("【CursorUtils】游标解析错误 - 缺少必要字段: " + json);
                return null;
            }
            
            // 处理Long转Date的问题
            Object timestampObj = cursorMap.get("timestamp");
            // 时间戳类型兼容处理
            
            if (timestampObj instanceof Long) {
                cursorMap.put("timestamp", new Date((Long) timestampObj));
            } else if (timestampObj instanceof Integer) {
                cursorMap.put("timestamp", new Date(((Integer) timestampObj).longValue()));
            } else if (timestampObj instanceof String) {
                // 尝试解析字符串格式的时间戳
                try {
                    Long timestamp = Long.parseLong((String) timestampObj);
                    cursorMap.put("timestamp", new Date(timestamp));
                } catch (NumberFormatException e) {
                    // 无法转换则保持原值
                }
            }
            
            // 确保ID是整数
            Object idObj = cursorMap.get("id");
            if (!(idObj instanceof Integer)) {
                if (idObj instanceof Number) {
                    cursorMap.put("id", ((Number) idObj).intValue());
                } else if (idObj instanceof String) {
                    try {
                        cursorMap.put("id", Integer.parseInt((String) idObj));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            
            return cursorMap;
        } catch (Exception e) {
            // 解析失败返回null
            return null;
        }
    }
}