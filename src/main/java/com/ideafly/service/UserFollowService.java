package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ideafly.dto.user.UserFollowInputDto;
import com.ideafly.dto.user.UserFollowStatusDto;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.model.users.UserFollow;
import com.ideafly.model.users.Users;

import java.util.List;

/**
 * 用户关注服务
 */
public interface UserFollowService extends IService<UserFollow> {

    /**
     * 关注用户
     * @param userId 关注者用户ID
     * @param targetUserId 被关注者用户ID
     */
    void followUser(String userId, String targetUserId);

    /**
     * 取消关注用户
     * @param userId 关注者用户ID
     * @param targetUserId 被关注者用户ID
     */
    void unfollowUser(String userId, String targetUserId);

    /**
     * 获取粉丝用户ID列表
     * @param userId 用户ID
     * @return 粉丝用户ID列表
     */
    List<String> getFollowersUserIds(String userId);

    /**
     * 判断用户是否已关注某用户
     * @param userId 关注者用户ID
     * @param targetUserId 被关注者用户ID
     * @return 如果已关注返回true，否则返回false
     */
    boolean isFollowing(String userId, String targetUserId);

    /**
     * 添加或取消关注
     */
    UserFollowStatusDto toggleFollow(UserFollowInputDto dto);
    
    /**
     * 检查当前用户是否关注了指定用户
     */
    boolean isFollowing(String followedId);
    
    /**
     * 获取当前用户关注的用户ID列表
     * @param userId 用户ID
     * @return 关注的用户ID列表
     */
    List<String> getFollowingUserIds(String userId);
    
    /**
     * 获取用户的关注统计信息
     */
    UserFollowStatsDto getUserFollowStats(String userId);
    
    /**
     * 获取用户的关注列表 (该用户关注的人)
     */
    List<Users> getUserFollowing(String userId);
    
    /**
     * 获取用户的粉丝列表 (关注该用户的人)
     */
    List<Users> getUserFollowers(String userId);
    
    /**
     * 获取用户的互关列表 (互相关注的用户)
     */
    List<Users> getMutualFollows(String userId);
} 