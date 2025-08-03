package com.ideafly.common;

import com.ideafly.dto.auth.LoginUser;
import com.ideafly.dto.user.UserDto;
import org.springframework.beans.BeanUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 请求工具类，用于从HttpServletRequest中获取用户信息
 */
public class RequestUtils {

    /**
     * 从请求中获取当前登录用户
     */
    public static LoginUser getCurrentUser(HttpServletRequest request) {
        return (LoginUser) request.getAttribute("loginUser");
    }

    /**
     * 从请求中获取当前用户ID
     */
    public static String getCurrentUserId(HttpServletRequest request) {
        LoginUser loginUser = getCurrentUser(request);
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * 从请求中获取当前用户信息（UserDto格式）
     */
    public static UserDto getCurrentUserDto(HttpServletRequest request) {
        LoginUser loginUser = getCurrentUser(request);
        if (loginUser == null) {
            return null;
        }
        
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(loginUser, userDto);
        return userDto;
    }
} 