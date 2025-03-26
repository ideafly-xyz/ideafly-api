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
}
