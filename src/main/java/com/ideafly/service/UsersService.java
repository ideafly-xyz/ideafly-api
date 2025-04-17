package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.user.UpdateUserInputDto;
import com.ideafly.mapper.UsersMapper;
import com.ideafly.model.Users;
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

    public void updateUser(UpdateUserInputDto userDto) {
        Users users = BeanUtil.copyProperties(userDto, Users.class);
        users.setId(UserContextHolder.getUid());
        this.updateById(users);
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
