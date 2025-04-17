package com.ideafly.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}") // 从配置文件中读取密钥
    private String secretKey;

    @Value("${jwt.expiration}") // Token 过期时间，单位毫秒
    private long jwtExpiration;

    @Value("${jwt.refreshExpiration}") // Refresh Token 过期时间，单位毫秒
    private long refreshExpiration;

    /**
     * 从令牌中获取用户ID
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中获取过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从令牌中获取指定声明
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从令牌中获取所有声明
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    /**
     * 检查令牌是否过期
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 生成令牌
     * @param userId 用户ID
     * @param isRefreshToken 是否是刷新令牌
     * @return 令牌
     */
    public String generateToken(String userId, boolean isRefreshToken) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("isRefreshToken", isRefreshToken);
        return createToken(claims, userId, isRefreshToken ? refreshExpiration : jwtExpiration);
    }

    /**
     * 创建令牌
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证令牌
     */
    public Boolean validateToken(String token, String userId) {
        final String extractedUserId = extractUserId(token);
        return (extractedUserId.equals(userId) && !isTokenExpired(token));
    }

    private Key getSignInKey() {
        // 替换 '-' 和 '_' 为 '+' 和 '/'
        String processedKey = secretKey.replace('-', '+').replace('_', '/');
        byte[] keyBytes = Decoders.BASE64.decode(processedKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从令牌中获取手机号（与extractUserId相同，但命名更符合TokenInterceptor需求）
     */
    public String extractPhoneNumber(String token) {
        return extractUserId(token);
    }

    /**
     * 验证手机号的令牌（与validateToken相同，但命名更符合TokenInterceptor需求）
     */
    public boolean isTokenValid(String token, String phoneNumber) {
        return validateToken(token, phoneNumber);
    }

    /**
     * 验证refresh token有效性
     */
    public boolean isRefreshTokenValid(String token, String phoneNumber) {
        try {
            // 验证token基本有效性
            if (!validateToken(token, phoneNumber)) {
                return false;
            }
            
            // 获取token类型，检查是否为refresh token
            final Claims claims = extractAllClaims(token);
            Boolean isRefreshToken = claims.get("isRefreshToken", Boolean.class);
            return isRefreshToken != null && isRefreshToken;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 生成普通token (无boolean参数版本，兼容旧代码)
     */
    public String generateToken(String phoneNumber) {
        return generateToken(phoneNumber, false);
    }
    
    /**
     * 生成刷新token
     */
    public String generateRefreshToken(String phoneNumber) {
        return generateToken(phoneNumber, true);
    }
}