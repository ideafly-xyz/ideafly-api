package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ideafly.dto.user.UserFollowInputDto;
import com.ideafly.dto.user.UserFollowStatusDto;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.model.UserFollow;
import com.ideafly.model.Users;

import java.util.List;

/**
 * 用户关注服务
 */
public interface UserFollowService extends IService<UserFollow> {

    /**
     * 添加或取消关注
     */
    UserFollowStatusDto toggleFollow(UserFollowInputDto dto);
    
    /**
     * 检查当前用户是否关注了指定用户
     */
    boolean isFollowing(Integer followedId);
    
    /**
     * 获取当前用户关注的用户ID列表
     * @param userId 用户ID
     * @return 关注的用户ID列表
     */
    List<Integer> getFollowingUserIds(Integer userId);
    
    /**
     * 获取用户的关注统计信息
     */
    UserFollowStatsDto getUserFollowStats(Integer userId);
    
    /**
     * 获取用户的关注列表 (该用户关注的人)
     */
    List<Users> getUserFollowing(Integer userId);
    
    /**
     * 获取用户的粉丝列表 (关注该用户的人)
     */
    List<Users> getUserFollowers(Integer userId);
    
    /**
     * 获取用户的互关列表 (互相关注的用户)
     */
    List<Users> getMutualFollows(Integer userId);
} 