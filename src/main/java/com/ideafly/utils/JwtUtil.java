package com.ideafly.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.jsonwebtoken.ExpiredJwtException;

/**
 * JWT工具类
 */
@Component
@Slf4j
public class JwtUtil {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret}") // 从配置文件中读取密钥
    private String secretKey;

    @Value("${jwt.expiration}") // Token 过期时间，单位毫秒
    private long jwtExpiration;

    @Value("${jwt.refreshExpiration}") // Refresh Token 过期时间，单位毫秒
    private long refreshExpiration;

    // Token黑名单前缀
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

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
        if (isRefreshToken) {
            claims.put("isRefreshToken", true);
            claims.put("tokenType", "refresh"); // 确保同时设置tokenType字段
        }
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
     * 从token中提取手机号，即使token已过期
     * 专用于refreshToken处理，允许从过期token中提取信息
     */
    public String extractPhoneNumberIgnoreExpired(String token) {
        try {
            return extractUserId(token);
        } catch (ExpiredJwtException e) {
            // 从过期的JWT中提取Claims
            return e.getClaims().getSubject();
        } catch (Exception e) {
            log.error("从token中提取phoneNumber失败：", e);
            throw e;
        }
    }

    /**
     * 验证手机号的令牌（与validateToken相同，但命名更符合TokenInterceptor需求）
     */
    public boolean isTokenValid(String token, String phoneNumber) {
        if (isTokenBlacklisted(token)) {
            return false;
        }
        
        String extractedPhoneNumber = extractPhoneNumber(token);
        return (extractedPhoneNumber.equals(phoneNumber) && !isTokenExpired(token));
    }

    /**
     * 验证refresh token有效性
     */
    public boolean isRefreshTokenValid(String token, String phoneNumber) {
        // 检查token是否在黑名单中
        if (isTokenBlacklisted(token)) {
            log.info("RefreshToken在黑名单中: {}", token);
            return false;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            
            // 支持两种类型的刷新令牌标识：
            // 1. 新方式，使用tokenType="refresh"
            // 2. 旧方式，使用isRefreshToken=true
            boolean isRefresh = false;
            
            // 检查tokenType字段
            String tokenType = (String) claims.get("tokenType");
            if ("refresh".equals(tokenType)) {
                isRefresh = true;
            }
            
            // 如果没有tokenType或tokenType不是"refresh"，检查isRefreshToken字段
            if (!isRefresh) {
                Boolean isRefreshToken = claims.get("isRefreshToken", Boolean.class);
                isRefresh = Boolean.TRUE.equals(isRefreshToken);
            }
            
            if (!isRefresh) {
                log.info("非刷新token类型");
                return false;
            }
            
            // 验证主题与phoneNumber是否匹配
            String extractedPhoneNumber = claims.getSubject();
            if (!extractedPhoneNumber.equals(phoneNumber)) {
                log.info("phoneNumber不匹配: token={}, input={}", extractedPhoneNumber, phoneNumber);
                return false;
            }
            
            // 检查token是否过期
            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());
            if (expired) {
                log.info("refreshToken已过期，过期时间: {}", expiration);
                return false;
            }
            
            return true;
        } catch (ExpiredJwtException e) {
            log.info("refreshToken已过期: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("验证refreshToken时发生异常", e);
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
        Map<String, Object> claims = new HashMap<>();
        claims.put("isRefreshToken", true);
        claims.put("tokenType", "refresh");
        return createToken(claims, phoneNumber, refreshExpiration);
    }

    /**
     * 使refreshToken失效
     */
    public void invalidateRefreshToken(String token) {
        if (token == null) return;
        
        try {
            // 解析token获取失效时间
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            
            // 计算剩余有效期（毫秒）
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                // 将token加入黑名单，保留到原过期时间
                redisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    ttl,
                    TimeUnit.MILLISECONDS
                );
                log.info("Token已加入黑名单，剩余有效期: {}ms", ttl);
            }
        } catch (Exception e) {
            log.error("使token失效时出错", e);
        }
    }

    /**
     * 检查token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }
}