package com.ideafly.utils;

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
        
        String cursorString = timestamp.getTime() + ":" + id;
        return Base64.getEncoder().encodeToString(cursorString.getBytes());
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
            // 解码base64
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String decodedString = new String(decodedBytes);
            
            // 分割时间戳和ID
            String[] parts = decodedString.split(":");
            if (parts.length != 2) {
                return null;
            }
            
            // 解析值
            long timestamp = Long.parseLong(parts[0]);
            int id = Integer.parseInt(parts[1]);
            
            // 创建结果Map
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", new Date(timestamp));
            result.put("id", id);
            
            return result;
        } catch (Exception e) {
            System.out.println("解析游标失败: " + e.getMessage());
            return null;
        }
    }
}