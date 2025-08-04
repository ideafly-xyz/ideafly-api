package com.ideafly.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Component
@Slf4j
public class JwtUtil {
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Value("${jwt.secret}") // 从配置文件中读取密钥
    private String secretKey;
    
    @Value("${jwt.expiration}") // Token 过期时间，单位毫秒
    private long jwtExpiration;
    
    @Value("${jwt.refreshExpiration}") // Refresh Token 过期时间，单位毫秒
    private long refreshExpiration;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "refreshTokenBlackList:";
    

    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    
    public String generateToken(String userId, boolean isRefreshToken) {
        Map<String, Object> claims = new HashMap<>();
        
        // 设置token类型
        if (isRefreshToken) {
            claims.put("tokenType", "refresh");
        } else {
            claims.put("tokenType", "access");
        }
        
        // 设置过期时间
        long expiration = isRefreshToken ? refreshExpiration : jwtExpiration;
        
        String token = createToken(claims, userId, expiration);
        
        // 记录生成的JWT详情（iat/exp为可读时间）
        try {
            Claims tokenClaims = extractAllClaims(token);
            Map<String, Object> logMap = new HashMap<>(tokenClaims);
            Object iatObj = logMap.get("iat");
            Object expObj = logMap.get("exp");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String iatReadable = iatObj != null ? sdf.format(new java.util.Date(((Number) iatObj).longValue() * 1000)) : "";
            String expReadable = expObj != null ? sdf.format(new java.util.Date(((Number) expObj).longValue() * 1000)) : "";
            logMap.put("iat(readable)", iatReadable);
            logMap.put("exp(readable)", expReadable);
            log.info("JWT解析详情 - {}: {{header={}, payload={}, signature=***}}", 
                    isRefreshToken ? "refreshToken" : "accessToken",
                    logMap.toString());
        } catch (Exception e) {
            log.warn("无法解析生成的JWT进行日志记录: {}", e.getMessage());
        }
        
        return token;
    }
    
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public Key getSignInKey() {
        // 替换 '-' 和 '_' 为 '+' 和 '/'
        String processedKey = secretKey.replace('-', '+').replace('_', '/');
        byte[] keyBytes = java.util.Base64.getDecoder().decode(processedKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /*
     * 从令牌中获取用户ID，即使token已过期
     * 专用于refreshToken处理，允许从过期token中提取信息
     */
    public String extractUserIdIgnoreExpired(String token) {
        try {
            return extractUserId(token);
        } catch (ExpiredJwtException e) {
            // 从过期的JWT中提取Claims
            return e.getClaims().getSubject();
        } catch (Exception e) {
            log.error("从token中提取userId失败：", e);
            throw e;
        }
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    //====accessToken====
    public Boolean isAccessTokenValid(String token) {
        try {            
            Claims claims = extractAllClaims(token);
            String tokenType = (String) claims.get("tokenType");
            if (!"access".equals(tokenType)) {
                log.info("非accessToken类型，tokenType: {}", tokenType);
                return false;
            }

            // 检查token是否过期
            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());
            if (expired) {
                log.info("accessToken已过期，过期时间: {}", expiration);
                return false;
            }
            return true;
        } catch (ExpiredJwtException e) {
            log.info("Token已过期: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.info("Token无效或已过期: {}", e.getMessage());
            return false;
        }
    }

    //====refreshToken=====

    /**
     * 验证refresh token有效性
     */
    public boolean isRefreshTokenValid(String token) {
        // 检查token是否在黑名单中
        if (isTokenBlacklisted(token)) {
            log.info("refreshToken在黑名单中: {}", token);
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            // 只支持新的tokenType方式
            String tokenType = (String) claims.get("tokenType");
            if (!"refresh".equals(tokenType)) {
                log.info("非刷新token类型，tokenType: {}", tokenType);
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
                stringRedisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    ttl,
                    TimeUnit.MILLISECONDS
                );

                // 截取token的前8位和后8位用于日志显示  
                String tokenForLog = token.length() > 16 ?   
                    token.substring(0, 8) + "..." + token.substring(token.length() - 8) : token;  

                log.info("refreshToken已加入黑名单，redis key: {}{}, value: {}, redis key 剩余有效期: {}ms",   
                    TOKEN_BLACKLIST_PREFIX, tokenForLog, "1", ttl);
            }
        } catch (Exception e) {
            log.error("使token失效时出错", e);
        }
    }

    /**
     * 检查token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }
}
