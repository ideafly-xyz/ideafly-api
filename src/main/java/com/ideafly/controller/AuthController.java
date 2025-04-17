package com.ideafly.controller;

import com.ideafly.dto.auth.TelegramAuthDto;
import com.ideafly.dto.auth.TokenDto;
import com.ideafly.service.AuthService;
import com.ideafly.common.ResponseResult;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Telegram登录
     */
    @PostMapping("/telegram/login")
    public ResponseResult<TokenDto> telegramLogin(@RequestBody @Valid TelegramAuthDto telegramAuthDto) {
        log.info("Telegram登录请求: {}", telegramAuthDto);
        
        // 调用服务进行登录
        TokenDto tokenDto = authService.telegramLogin(telegramAuthDto);
        return ResponseResult.success(tokenDto);
    }
} 