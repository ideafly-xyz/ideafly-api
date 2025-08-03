package com.ideafly.service.impl.users;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.dto.user.UpdateUserInputDto;
import com.ideafly.mapper.users.UsersMapper;
import com.ideafly.model.users.Users;

import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author rfs
 * @date 2025/03/10
 * 用户服务
 */
@Service
public class UsersService extends ServiceImpl<UsersMapper, Users> {
    public Users getUserByMobile(String mobile) {
        return this.lambdaQuery().eq(Users::getMobile, mobile).one();
    }

    public void updateUser(UpdateUserInputDto userDto, String userId) {
        System.out.println("【服务调试日志】接收到的UpdateUserInputDto: " + userDto);
        System.out.println("【服务调试日志】个人简介personalBio原始值: " + userDto.getPersonalBio());
        
        // 打印来自前端的所有DTO字段，帮助诊断
        System.out.println("【服务调试日志】DTO字段详情：");
        System.out.println("- nickname: " + userDto.getNickname());
        System.out.println("- personalBio: " + userDto.getPersonalBio());
        System.out.println("- location: " + userDto.getLocation());
        System.out.println("- websiteUrl: " + userDto.getWebsiteUrl());
        System.out.println("- gender: " + userDto.getGender());
        
        Users users = BeanUtil.copyProperties(userDto, Users.class);
        System.out.println("【服务调试日志】复制属性后的Users对象: " + users);
        System.out.println("【服务调试日志】复制属性后的bio值: " + users.getBio());
        
        // 特殊处理personalBio到bio的映射
        if (userDto.getPersonalBio() != null) {
            users.setBio(userDto.getPersonalBio());
            System.out.println("【服务调试日志】手动设置后的bio值: " + users.getBio());
        } else {
            System.out.println("【服务调试日志】personalBio为null，不进行映射");
        }
        
        users.setId(userId);
        boolean updated = update(users, new UpdateWrapper<Users>().eq("id", users.getId()));
        System.out.println("【服务调试日志】数据库更新结果: " + updated);
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
