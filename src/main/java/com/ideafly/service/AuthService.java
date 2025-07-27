package com.ideafly.service;

import com.ideafly.dto.auth.LoginUser;
import com.ideafly.dto.auth.TelegramAuthDto;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * Telegram登录
     * @param authData Telegram认证数据
     * @return 包含refreshToken的结果
     */
    Map<String, String> telegramLogin(TelegramAuthDto authData);
    
    /**
     * 通过refreshToken获取accessToken
     * @param refreshToken 刷新令牌
     * @return 包含accessToken的结果
     */
    Map<String, String> getAccessToken(String refreshToken);
    
    /**
     * 从token中获取用户信息
     * @param token 用户令牌
     * @return 登录用户信息
     */
    LoginUser getUserByToken(String token);
    
    /**
     * 验证token是否有效
     * @param token 用户令牌
     * @return 是否有效
     */
    boolean validateToken(String token);
}