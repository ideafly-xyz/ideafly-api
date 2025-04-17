package com.ideafly.service;

import com.ideafly.dto.auth.TelegramAuthDto;
import com.ideafly.dto.auth.TokenDto;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * Telegram登录
     * @param telegramAuthDto Telegram认证数据
     * @return 令牌和用户信息
     */
    TokenDto telegramLogin(TelegramAuthDto telegramAuthDto);
} 