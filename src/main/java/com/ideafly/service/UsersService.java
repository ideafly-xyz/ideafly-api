package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    public Users saveUserByMobile(String mobile) {
        Users user = this.getUserByMobile(mobile);
        if (Objects.isNull(user)) {
            user = new Users();
            user.setMobile(mobile);
            user.setUsername(mobile);
            this.save(user);
        }
        return user;
    }
}
