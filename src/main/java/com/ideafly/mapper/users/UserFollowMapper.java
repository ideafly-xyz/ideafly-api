package com.ideafly.mapper.users;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideafly.model.users.UserFollow;

import org.apache.ibatis.annotations.Mapper;

/**
 * 用户关注关系Mapper接口
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
} 