package com.ideafly.service;

import com.ideafly.entity.dto.LoginUser;
import com.ideafly.model.Users;

/**
 * Token管理服务接口
 */
public interface TokenManagerService {
    
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
    
    /**
     * 将Users实体转换为LoginUser对象
     * @param user 用户实体
     * @return 登录用户信息
     */
    LoginUser convertToLoginUser(Users user);
} 