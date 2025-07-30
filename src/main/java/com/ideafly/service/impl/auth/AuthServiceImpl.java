package com.ideafly.service.impl.auth;

import com.ideafly.dto.auth.LoginUser;
import com.ideafly.dto.auth.TelegramAuthDto;
import com.ideafly.model.users.Users;
import com.ideafly.service.AuthService;
import com.ideafly.utils.JwtUtil;
import com.ideafly.utils.TelegramAuthUtil;
import org.springframework.beans.factory.annotation.Value;
import com.ideafly.service.impl.users.UsersService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Resource
    private UsersService usersService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Override
    public Map<String, String> telegramLogin(TelegramAuthDto authData) {
        // 验证Telegram登录数据
        boolean isValid = TelegramAuthUtil.verify(authData, botToken);
        
        if (!isValid) {
            throw new RuntimeException("Telegram验证失败");
        }
        
        // 查找或创建用户
        Users user = getUserByTelegramId(authData);
        
        // 只生成refreshToken
        String userId = user.getId();
        String refreshToken = jwtUtil.generateToken(userId, true);
        
        Map<String, String> result = new HashMap<>();
        result.put("refreshToken", refreshToken);
        
        log.info("用户登录成功, userId: {}", userId);
        
        return result;
    }
    
    @Override
    public Map<String, String> getAccessToken(String refreshToken) {
        log.info("收到获取accessToken请求");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("refreshToken为空");
            throw new RuntimeException("refreshToken不能为空");
        }
        
        try {
            String userId = jwtUtil.extractUserIdIgnoreExpired(refreshToken);
            log.info("从refreshToken提取的userId: {}", userId);
            
            // 验证refreshToken是否有效 (包含过期验证)
            if (!jwtUtil.isRefreshTokenValid(refreshToken, userId)) { 
                log.warn("refreshToken 无效或已过期");
                throw new RuntimeException("refreshToken无效或已过期");
            }
            
            log.info("refreshToken有效，正在生成新的accessToken");
            
            // 生成新的accessToken
            String newAccessToken = jwtUtil.generateToken(userId, false);
            
            Map<String, String> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            
            log.info("成功生成新的accessToken");
            
            return result;
        } catch (Exception e) {
            log.error("获取访问令牌过程中发生异常", e);
            throw new RuntimeException("获取访问令牌失败: " + e.getMessage());
        }
    }
    
    @Override
    public LoginUser getUserByToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Token为空");
            return null;
        }
        
        // 先检查token是否在黑名单中
        if (jwtUtil.isTokenBlacklisted(token)) {
            log.warn("令牌已被加入黑名单");
            return null;
        }
        
        try {
            String userId = jwtUtil.extractUserId(token);
            log.debug("从Token中提取用户标识: {}", userId);
            
            // 验证token有效性
            if (!jwtUtil.isTokenValid(token, userId)) {
                log.warn("Token无效");
                return null;
            }
            
            // 直接用userId查找用户
            Users user = usersService.getById(userId);
            log.debug("通过UUID查找用户: {}", userId);
            
            if (user == null) {
                log.warn("未找到用户: {}", userId);
                return null;
            }
            
            return convertToLoginUser(user);
        } catch (Exception e) {
            log.error("从token获取用户信息失败", e);
            return null;
        }
    }
    
    @Override
    public boolean validateToken(String token) {
        // 先检查token是否在黑名单中
        if (jwtUtil.isTokenBlacklisted(token)) {
            log.warn("令牌已被加入黑名单");
            return false;
        }
        
        try {
            String userId = jwtUtil.extractUserId(token);
            return jwtUtil.isTokenValid(token, userId);
        } catch (Exception e) {
            log.error("验证token失败", e);
            return false;
        }
    }
    
    /**
     * 通过Telegram ID查找用户，如果不存在则创建新用户
     */
    private Users getUserByTelegramId(TelegramAuthDto authData) {
        // 查找是否已存在该Telegram ID的用户
        Users user = usersService.lambdaQuery()
                .eq(Users::getTelegramId, authData.getId())
                .one();
        
        // 如果用户不存在，创建新用户
        if (user == null) {
            user = new Users();
            
            // 设置Telegram相关信息
            user.setTelegramId(authData.getId());
            user.setUsername(authData.getFirstName() + 
                             (authData.getUsername() != null ? " " + authData.getUsername() : ""));
            
            // 设置头像
            if (authData.getPhotoUrl() != null) {
                user.setAvatar(authData.getPhotoUrl());
            }
            
            // 设置注册时间
            user.setCreatedAt(new Date());
            user.setUpdatedAt(new Date());
            
            // 保存用户
            usersService.save(user);
        } else {
            // 更新登录时间
            user.setUpdatedAt(new Date());
            usersService.updateById(user);
        }
        
        return user;
    }
    
    /**
     * 将Users实体转换为LoginUser对象
     */
    private LoginUser convertToLoginUser(Users user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setEmail(user.getEmail());
        loginUser.setRole(user.getRole());
        loginUser.setMobile(user.getMobile());
        
        return loginUser;
    }
}