package com.ideafly.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 令牌数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 用户信息
     */
    private Object userInfo;
} 