package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.mapper.UsersMapper;
import com.ideafly.model.Users;
import org.springframework.stereotype.Service;

/**
 * @author rfs
 * @date 2025/03/10
 * 用户服务
 */
@Service
public class UsersService extends ServiceImpl<UsersMapper, Users> {
}
