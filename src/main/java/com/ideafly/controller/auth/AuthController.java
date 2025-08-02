package com.ideafly.controller.auth;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.ErrorCode;
import com.ideafly.common.R;
import com.ideafly.dto.auth.TelegramAuthDto;
import com.ideafly.service.impl.auth.AuthServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Tag(name = "认证接口", description = "用户认证相关接口")
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    
    @Resource
    private AuthServiceImpl authService;
    
    @NoAuth
    @PostMapping("/login")
    @Operation(summary = "Telegram登录", description = "使用Telegram认证登录，返回accessToken和refreshToken")
    public R<Map<String, String>> login(@Valid @RequestBody TelegramAuthDto authData) {
        log.info("收到Telegram登录请求参数: {}", authData);
        try {
            Map<String, String> result = authService.telegramLogin(authData);
            return R.success(result);
        } catch (Exception e) {
            log.error("Telegram登录失败", e);
            return R.error("Telegram登录失败: " + e.getMessage());
        }
    }
    
    @NoAuth
    @PostMapping("/access-token")
    @Operation(summary = "获取访问令牌", description = "通过refreshToken获取新的accessToken和refreshToken")
    public R<Map<String, String>> getAccessToken(HttpServletRequest request) {
        
        String refreshToken = null;
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            refreshToken = authorizationHeader.substring(7); // 去除 "Bearer " 前缀
        }
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("refreshToken为空");
            return R.error(ErrorCode.TOKEN_NULL);
        }
        
        try {
            Map<String, String> result = authService.getAccessToken(refreshToken);
            return R.success(result);
        } catch (Exception e) {
            log.error("获取访问令牌失败", e);
            return R.error(ErrorCode.SYSTEM_ERROR.getCode(), "获取访问令牌失败: " + e.getMessage());
        }
    }
}