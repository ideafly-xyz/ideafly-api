package com.ideafly.service.impl;

import com.ideafly.entity.dto.LoginUser;
import com.ideafly.model.Users;
import com.ideafly.service.TokenManagerService;
import com.ideafly.service.UsersService;
import com.ideafly.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * Token管理服务实现类
 * 用于处理Token验证和用户信息获取
 */
@Service
public class TokenManagerServiceImpl implements TokenManagerService {

    private static final Logger logger = LoggerFactory.getLogger(TokenManagerServiceImpl.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Autowired
    private UsersService usersService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 从token中提取用户信息
     * @param token JWT令牌
     * @return 登录用户信息，如果token无效则返回null
     */
    @Override
    public LoginUser getUserByToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Token为空");
            return null;
        }
        
        // 先检查token是否在黑名单中
        if (jwtUtil.isTokenBlacklisted(token)) {
            logger.warn("令牌已被加入黑名单");
            return null;
        }
        
        // 记录token信息，帮助调试（生产环境应移除或脱敏）
        logger.debug("Token长度: {}, 前10位: {}", token.length(), 
                token.length() > 10 ? token.substring(0, 10) + "..." : token);
        
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        
        String userIdentifier = claims.getSubject();
        logger.debug("从Token中提取用户标识: {}", userIdentifier);
        
        // 直接用userIdentifier作为uuid查找用户
        Users user = usersService.getById(userIdentifier);
        logger.debug("通过UUID查找用户: {}", userIdentifier);
        
        if (user == null) {
            logger.warn("未找到用户: {}", userIdentifier);
            return null;
        }
        
        return convertToLoginUser(user);
    }
    
    /**
     * 验证token是否有效
     * @param token JWT令牌
     * @return 如果token有效则返回true，否则返回false
     */
    @Override
    public boolean validateToken(String token) {
        // 先检查token是否在黑名单中
        if (jwtUtil.isTokenBlacklisted(token)) {
            logger.warn("令牌已被加入黑名单");
            return false;
        }
        
        return parseToken(token) != null;
    }
    
    private Claims parseToken(String token) {
        try {
            // 检查token格式
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.error("无效的JWT格式: 部分数量不正确");
                return null;
            }
            
            // 将密钥转换为Base64格式
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret.getBytes());
            
            return Jwts.parserBuilder()
                    .setSigningKey(keyBytes)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT解析异常: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 将Users实体转换为LoginUser对象
     */
    @Override
    public LoginUser convertToLoginUser(Users user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setEmail(user.getEmail());
        loginUser.setRole(user.getRole());
        loginUser.setMobile(user.getMobile());
        
        return loginUser;
    }
} 