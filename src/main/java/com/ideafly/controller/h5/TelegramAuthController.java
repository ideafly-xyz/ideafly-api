package com.ideafly.controller.h5;

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
@RequestMapping("api/auth/telegram")
public class TelegramAuthController {
    
    @Resource
    private TelegramAuthService telegramAuthService;
    
    @Resource
    private UsersService usersService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @NoAuth
    @PostMapping("/h5/login")
    @Operation(summary = "H5端Telegram登录")
    public R<LoginSuccessOutputDto> telegramLogin(@Valid @RequestBody TelegramAuthDto authData) {
        // 验证Telegram登录数据
        boolean isValid = telegramAuthService.verifyTelegramAuth(authData);
        
        if (!isValid) {
            return R.error("Telegram验证失败");
        }
        
        // 查找或创建用户
        Users user = getUserByTelegramId(authData);
        
        // 生成JWT tokens
        LoginSuccessOutputDto outputDto = new LoginSuccessOutputDto();
        outputDto.setAccessToken(jwtUtil.generateToken(user.getMobile()));
        outputDto.setRefreshToken(jwtUtil.generateRefreshToken(user.getMobile()));
        outputDto.setUserInfo(BeanUtil.copyProperties(user, UserDto.class));
        
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
} 