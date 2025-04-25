package com.ideafly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.user.UserFollowInputDto;
import com.ideafly.dto.user.UserFollowStatusDto;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.mapper.UserFollowMapper;
import com.ideafly.model.UserFollow;
import com.ideafly.model.Users;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户关注服务
 */
@Service
public class UserFollowService extends ServiceImpl<UserFollowMapper, UserFollow> {

    @Resource
    private UsersService usersService;

    /**
     * 添加或取消关注
     */
    public UserFollowStatusDto toggleFollow(UserFollowInputDto dto) {
        Integer followerId = UserContextHolder.getUid();
        Integer followedId = dto.getFollowedId();
        
        // 不允许关注自己
        if (Objects.equals(followerId, followedId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        
        // 检查被关注的用户是否存在
        Users followedUser = usersService.getById(followedId);
        if (followedUser == null) {
            throw new IllegalArgumentException("被关注的用户不存在");
        }
        
        // 查找是否已存在关注记录
        UserFollow userFollow = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowedId, followedId)
                .one();
        
        // 构建返回对象
        UserFollowStatusDto result = new UserFollowStatusDto();
        result.setFollowerId(followerId);
        result.setFollowedId(followedId);
        
        // 如果记录不存在，创建新的关注关系
        if (userFollow == null) {
            userFollow = new UserFollow();
            userFollow.setFollowerId(followerId);
            userFollow.setFollowedId(followedId);
            userFollow.setCreatedAt(new Date());
            userFollow.setStatus(1); // 激活状态
            this.save(userFollow);
            result.setFollowing(true);
        } else {
            // 如果记录存在，切换关注状态
            int newStatus = userFollow.getStatus() == 1 ? 0 : 1;
            userFollow.setStatus(newStatus);
            this.updateById(userFollow);
            result.setFollowing(newStatus == 1);
        }
        
        return result;
    }
    
    /**
     * 检查当前用户是否关注了指定用户
     */
    public boolean isFollowing(Integer followedId) {
        Integer followerId = UserContextHolder.getUid();
        if (followerId == null || followedId == null) {
            return false;
        }
        
        // 查询关注记录
        UserFollow userFollow = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowedId, followedId)
                .eq(UserFollow::getStatus, 1) // 只查询激活状态的关注
                .one();
        
        return userFollow != null;
    }
    
    /**
     * 获取用户的关注统计信息
     */
    public UserFollowStatsDto getUserFollowStats(Integer userId) {
        if (userId == null) {
            userId = UserContextHolder.getUid();
        }
        
        // 获取用户信息
        Users user = usersService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        // 查询关注数量 (用户关注的人数)
        long followingCount = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1) // 只统计激活状态的关注
                .count();
        
        // 查询粉丝数量 (关注该用户的人数)
        long followersCount = this.lambdaQuery()
                .eq(UserFollow::getFollowedId, userId)
                .eq(UserFollow::getStatus, 1) // 只统计激活状态的关注
                .count();
        
        // 查询互相关注数量
        // 1. 先获取该用户关注的人
        List<Integer> followingIds = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());
        
        // 2. 如果没有关注任何人，互关数量为0
        long mutualCount = 0;
        if (!followingIds.isEmpty()) {
            // 3. 在这些人中查找有多少人也关注了该用户
            mutualCount = this.lambdaQuery()
                    .eq(UserFollow::getFollowedId, userId) // 关注了该用户
                    .in(UserFollow::getFollowerId, followingIds) // 且被该用户关注
                    .eq(UserFollow::getStatus, 1) // 只统计激活状态的关注
                    .count();
        }
        
        // 构建并返回统计DTO
        UserFollowStatsDto stats = new UserFollowStatsDto();
        stats.setUserId(userId);
        stats.setUsername(user.getUsername());
        stats.setFollowersCount((int) followersCount);
        stats.setFollowingCount((int) followingCount);
        stats.setMutualFollowCount((int) mutualCount);
        
        return stats;
    }
    
    /**
     * 获取用户的关注列表 (该用户关注的人)
     */
    public List<Users> getUserFollowing(Integer userId) {
        if (userId == null) {
            userId = UserContextHolder.getUid();
        }
        
        // 获取用户关注的人的ID列表
        List<Integer> followingIds = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());
        
        // 如果没有关注任何人，返回空列表
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些用户的信息
        return usersService.listByIds(followingIds);
    }
    
    /**
     * 获取用户的粉丝列表 (关注该用户的人)
     */
    public List<Users> getUserFollowers(Integer userId) {
        if (userId == null) {
            userId = UserContextHolder.getUid();
        }
        
        // 获取关注该用户的人的ID列表
        List<Integer> followerIds = this.lambdaQuery()
                .eq(UserFollow::getFollowedId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());
        
        // 如果没有粉丝，返回空列表
        if (followerIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些用户的信息
        return usersService.listByIds(followerIds);
    }
    
    /**
     * 获取互相关注的用户列表
     */
    public List<Users> getMutualFollows(Integer userId) {
        if (userId == null) {
            userId = UserContextHolder.getUid();
        }
        
        // 获取用户关注的人的ID列表
        List<Integer> followingIds = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());
        
        // 如果没有关注任何人，返回空列表
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 在这些人中查找有哪些人也关注了该用户
        List<Integer> mutualIds = this.lambdaQuery()
                .eq(UserFollow::getFollowedId, userId) // 关注了该用户
                .in(UserFollow::getFollowerId, followingIds) // 且被该用户关注
                .eq(UserFollow::getStatus, 1) // 只统计激活状态的关注
                .list()
                .stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());
        
        // 如果没有互相关注的用户，返回空列表
        if (mutualIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取这些用户的信息
        return usersService.listByIds(mutualIds);
    }
} 