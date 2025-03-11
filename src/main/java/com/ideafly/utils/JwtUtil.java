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

@Component
public class JwtUtil {

    @Value("${jwt.secret}") // 从配置文件中读取密钥
    private String secretKey;

    @Value("${jwt.expiration}") // Token 过期时间，单位毫秒
    private long jwtExpiration;

    @Value("${jwt.refreshExpiration}") // Refresh Token 过期时间，单位毫秒
    private long refreshExpiration;


    public String generateToken(String phoneNumber) {
        return generateToken(new HashMap<>(), phoneNumber);
    }

    public String generateRefreshToken(String phoneNumber) {
        return generateRefreshToken(new HashMap<>(), phoneNumber);
    }


    public String generateToken(Map<String, Object> extraClaims, String phoneNumber) {
        return buildToken(extraClaims, phoneNumber, jwtExpiration);
    }

    public String generateRefreshToken(Map<String, Object> extraClaims, String phoneNumber) {
        return buildToken(extraClaims, phoneNumber, refreshExpiration);
     }


    private String buildToken(Map<String, Object> extraClaims, String phoneNumber, long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(phoneNumber) // 使用手机号作为 Subject
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String phoneNumber) {
        final String tokenPhoneNumber = extractPhoneNumber(token);
        return (tokenPhoneNumber.equals(phoneNumber)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractPhoneNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        // 替换 '-' 和 '_' 为 '+' 和 '/'
        String processedKey = secretKey.replace('-', '+').replace('_', '/');
        byte[] keyBytes = Decoders.BASE64.decode(processedKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}