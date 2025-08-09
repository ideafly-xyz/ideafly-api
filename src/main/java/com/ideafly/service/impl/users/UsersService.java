package com.ideafly.service.impl.users;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.dto.user.UpdateUserInputDto;
import com.ideafly.mapper.users.UsersMapper;
import com.ideafly.model.users.Users;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author rfs
 * @date 2025/03/10
 * 用户服务
 */
@Service
@Slf4j
public class UsersService extends ServiceImpl<UsersMapper, Users> {
    public Users getUserByMobile(String mobile) {
        return this.lambdaQuery().eq(Users::getMobile, mobile).one();
    }

    public void updateUser(UpdateUserInputDto userDto, String userId) {
        log.info("【服务调试日志】接收到的UpdateUserInputDto: {}", userDto);
        log.info("【服务调试日志】个人简介personalBio原始值: {}", userDto.getPersonalBio());
        
        // 打印来自前端的所有DTO字段，帮助诊断
        log.debug("【服务调试日志】DTO字段详情：");
        log.debug("- nickname: {}", userDto.getNickname());
        log.debug("- personalBio: {}", userDto.getPersonalBio());
        log.debug("- location: {}", userDto.getLocation());
        log.debug("- websiteUrl: {}", userDto.getWebsiteUrl());
        log.debug("- gender: {}", userDto.getGender());
        
        Users users = BeanUtil.copyProperties(userDto, Users.class);
        log.debug("【服务调试日志】复制属性后的Users对象: {}", users);
        log.debug("【服务调试日志】复制属性后的bio值: {}", users.getBio());
        
        // 特殊处理personalBio到bio的映射
        if (userDto.getPersonalBio() != null) {
            users.setBio(userDto.getPersonalBio());
            log.debug("【服务调试日志】手动设置后的bio值: {}", users.getBio());
        } else {
            log.debug("【服务调试日志】personalBio为null，不进行映射");
        }
        
        users.setId(userId);
        boolean updated = update(users, new UpdateWrapper<Users>().eq("id", users.getId()));
        log.info("【服务调试日志】数据库更新结果: {}", updated);
    }

    public Users getOrAddByMobile(String mobile) {
        Users user = this.getUserByMobile(mobile);
        if (Objects.isNull(user)) {
            user = new Users();
            user.setMobile(mobile);
            user.setUsername(mobile);
            this.save(user);
        }
        return user;
    }
    
    public Users getUserByTelegramId(String telegramId) {
        return this.lambdaQuery().eq(Users::getTelegramId, telegramId).one();
    }
    
    public Users getOrAddByTelegramId(String telegramId, String firstName, String lastName) {
        Users user = this.getUserByTelegramId(telegramId);
        if (Objects.isNull(user)) {
            user = new Users();
            user.setTelegramId(telegramId);
            
            // 设置用户昵称
            String nickname = firstName;
            if (lastName != null && !lastName.isEmpty()) {
                nickname += " " + lastName;
            }
            user.setUsername(nickname);
            
            // 为用户ID设置唯一标识符 (Telegram_<ID的后5位>)
            user.setMobile("Telegram_" + telegramId.substring(Math.max(0, telegramId.length() - 5)));
            
            this.save(user);
        }
        return user;
    }
}
