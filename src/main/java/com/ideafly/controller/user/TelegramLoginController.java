package com.ideafly.controller.user;

import cn.hutool.core.bean.BeanUtil;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.dto.LoginSuccessOutputDto;
import com.ideafly.dto.TelegramAuthDto;
import com.ideafly.dto.user.UserDto;
import com.ideafly.model.Users;
import com.ideafly.service.TelegramAuthService;
import com.ideafly.service.UsersService;
import com.ideafly.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Date;

@Tag(name = "Telegram认证接口", description = "Telegram登录认证")
@RestController
@RequestMapping("api/user/telegram")
public class TelegramLoginController {
    
    @Resource
    private TelegramAuthService telegramAuthService;
    
    @Resource
    private UsersService usersService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @NoAuth
    @PostMapping("/login")
    @Operation(summary = "Telegram登录")
    public R<LoginSuccessOutputDto> telegramLogin(@Valid @RequestBody TelegramAuthDto authData) {
        // 验证Telegram登录数据
        boolean isValid = telegramAuthService.verifyTelegramAuth(authData);
        
        if (!isValid) {
            return R.error("Telegram验证失败");
        }
        
        // 查找或创建用户
        Users user = getUserByTelegramId(authData);
        
        // 生成JWT tokens
        String userId = user.getId();
        LoginSuccessOutputDto outputDto = new LoginSuccessOutputDto();
        String accessToken = jwtUtil.generateToken(userId, false);
        String refreshToken = jwtUtil.generateToken(userId, true);
        outputDto.setAccessToken(accessToken);
        outputDto.setRefreshToken(refreshToken);
        outputDto.setUserInfo(BeanUtil.copyProperties(user, UserDto.class));

        // 日志：输出token及其结构
        logJwtTokenDetail("accessToken", accessToken);
        logJwtTokenDetail("refreshToken", refreshToken);
        
        return R.success(outputDto);
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
                             (authData.getLastName() != null ? " " + authData.getLastName() : ""));
            
            // 设置头像
            if (authData.getPhotoUrl() != null) {
                user.setAvatar(authData.getPhotoUrl());
            }
            
            // 生成一个临时的唯一手机号，用于JWT
            user.setMobile("tg_" + authData.getId());
            
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

    private void logJwtTokenDetail(String label, String token) {
        try {
            System.out.println("==== " + label + " 结构分析 ====");
            System.out.println("完整token: " + token);
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                System.out.println("header(base64): " + parts[0]);
                System.out.println("payload(base64): " + parts[1]);
                System.out.println("signature(base64): " + parts[2]);
                java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
                String headerJson = new String(decoder.decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
                String payloadJson = new String(decoder.decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("header(json): " + headerJson);
                System.out.println("payload(json): " + payloadJson);
                System.out.println("signature(原始base64): " + parts[2]);
            } else {
                System.out.println("token格式不正确，无法分割为3段");
            }
        } catch (Exception e) {
            System.out.println("解析token日志异常: " + e.getMessage());
        }
    }
} 