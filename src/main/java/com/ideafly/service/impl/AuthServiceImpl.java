package com.ideafly.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ideafly.dto.auth.TelegramAuthDto;
import com.ideafly.dto.auth.TokenDto;
import com.ideafly.mapper.UsersMapper;
import com.ideafly.model.Users;
import com.ideafly.service.AuthService;
import com.ideafly.utils.JwtUtil;
import com.ideafly.utils.TelegramLoginVerifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private String telegramBotToken;

    private final UsersMapper usersMapper;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthServiceImpl(UsersMapper usersMapper, JwtUtil jwtUtil) {
        this.usersMapper = usersMapper;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Telegram登录
     * @param telegramAuthDto Telegram认证数据
     * @return 令牌和用户信息
     */
    @Override
    @Transactional
    public TokenDto telegramLogin(TelegramAuthDto telegramAuthDto) {
        log.info("处理Telegram登录: {}", telegramAuthDto);

        // 验证Telegram认证数据
        boolean isValid = TelegramLoginVerifier.verify(telegramAuthDto, telegramBotToken);
        if (!isValid) {
            log.error("Telegram认证数据验证失败");
            throw new RuntimeException("Telegram认证数据无效");
        }

        // 查询是否已存在用户（通过telegramId）
        String telegramId = telegramAuthDto.getId();
        Users user = findUserByTelegramId(telegramId);

        // 如果用户不存在，创建新用户
        if (user == null) {
            user = createUserFromTelegram(telegramAuthDto);
        } else {
            // 更新用户Telegram相关信息
            updateUserTelegramInfo(user, telegramAuthDto);
        }

        // 生成JWT令牌
        String accessToken = jwtUtil.generateToken(user.getId().toString(), false);
        String refreshToken = jwtUtil.generateToken(user.getId().toString(), true);

        // 构建用户信息
        Map<String, Object> userInfo = buildUserInfo(user);

        // 返回令牌和用户信息
        return new TokenDto(accessToken, refreshToken, userInfo);
    }

    /**
     * 根据Telegram ID查找用户
     */
    private Users findUserByTelegramId(String telegramId) {
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getTelegramId, telegramId);
        return usersMapper.selectOne(wrapper);
    }

    /**
     * 从Telegram数据创建新用户
     */
    private Users createUserFromTelegram(TelegramAuthDto telegramAuthDto) {
        Users user = new Users();
        
        // 设置Telegram相关信息
        user.setTelegramId(telegramAuthDto.getId());
        user.setTelegramUsername(telegramAuthDto.getUsername());
        
        // 设置基本用户信息
        user.setUsername(generateUsernameFromTelegram(telegramAuthDto));
        user.setFirstName(telegramAuthDto.getFirstName());
        user.setLastName(telegramAuthDto.getLastName());
        
        // 头像URL
        if (StringUtils.isNotBlank(telegramAuthDto.getPhotoUrl())) {
            user.setAvatar(telegramAuthDto.getPhotoUrl());
        }
        
        // 设置默认状态和创建时间
        user.setStatus(1); // 正常状态
        user.setRole(0);   // 普通用户角色
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        
        // 保存用户
        usersMapper.insert(user);
        return user;
    }

    /**
     * 从Telegram数据生成用户名
     */
    private String generateUsernameFromTelegram(TelegramAuthDto telegramAuthDto) {
        // 优先使用Telegram用户名，如果没有则使用"tg_"前缀和ID组合
        if (StringUtils.isNotBlank(telegramAuthDto.getUsername())) {
            return telegramAuthDto.getUsername();
        } else {
            return "tg_" + telegramAuthDto.getId();
        }
    }

    /**
     * 更新用户的Telegram相关信息
     */
    private void updateUserTelegramInfo(Users user, TelegramAuthDto telegramAuthDto) {
        boolean needUpdate = false;
        
        // 更新Telegram用户名（如果有变化）
        if (StringUtils.isNotBlank(telegramAuthDto.getUsername()) && 
            !telegramAuthDto.getUsername().equals(user.getTelegramUsername())) {
            user.setTelegramUsername(telegramAuthDto.getUsername());
            needUpdate = true;
        }
        
        // 更新名和姓（如果有变化且原值为空）
        if (StringUtils.isNotBlank(telegramAuthDto.getFirstName()) && 
            StringUtils.isBlank(user.getFirstName())) {
            user.setFirstName(telegramAuthDto.getFirstName());
            needUpdate = true;
        }
        
        if (StringUtils.isNotBlank(telegramAuthDto.getLastName()) && 
            StringUtils.isBlank(user.getLastName())) {
            user.setLastName(telegramAuthDto.getLastName());
            needUpdate = true;
        }
        
        // 更新头像（如果有变化且原值为空）
        if (StringUtils.isNotBlank(telegramAuthDto.getPhotoUrl()) && 
            StringUtils.isBlank(user.getAvatar())) {
            user.setAvatar(telegramAuthDto.getPhotoUrl());
            needUpdate = true;
        }
        
        // 有变更则更新
        if (needUpdate) {
            user.setUpdatedAt(new Date());
            usersMapper.updateById(user);
        }
    }

    /**
     * 构建用户信息
     */
    private Map<String, Object> buildUserInfo(Users user) {
        Map<String, Object> userInfo = new HashMap<>();
        // 使用snake_case格式，匹配前端UserModel的fromJson方法预期
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("first_name", user.getFirstName());
        userInfo.put("last_name", user.getLastName());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("telegram_id", user.getTelegramId());
        userInfo.put("telegram_username", user.getTelegramUsername());
        userInfo.put("status", user.getStatus());
        userInfo.put("role", user.getRole());
        // 添加用户资料字段
        userInfo.put("gender", user.getGender());
        userInfo.put("location", user.getLocation());
        userInfo.put("personal_bio", user.getBio()); // 前端使用personal_bio
        // 添加日期格式化
        userInfo.put("created_at", user.getCreatedAt() != null ? 
                     user.getCreatedAt().toString() : null);
        userInfo.put("updated_at", user.getUpdatedAt() != null ? 
                     user.getUpdatedAt().toString() : null);
        return userInfo;
    }
} 