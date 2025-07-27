package com.ideafly.mapper.users;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideafly.model.users.Users;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UsersMapper extends BaseMapper<Users> {
}
