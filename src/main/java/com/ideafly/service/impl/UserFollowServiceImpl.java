package com.ideafly.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.mapper.UserFollowMapper;
import com.ideafly.dto.user.UserFollowInputDto;
import com.ideafly.dto.user.UserFollowStatusDto;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.model.UserFollow;
import com.ideafly.model.Users;
import com.ideafly.service.UserFollowService;
import com.ideafly.service.UsersService;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements UserFollowService {

    @Resource
    private UsersService usersService;

    /**
     * 添加或取消关注
     */
    @Override
    public UserFollowStatusDto toggleFollow(UserFollowInputDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("未提供关注参数");
        }
        
        Integer followerId = UserContextHolder.getUid();
        Integer followedId = dto.getFollowedId();
        
        if (followedId == null) {
            throw new IllegalArgumentException("被关注者ID不能为空");
        }
        
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
     * 判断当前用户是否已关注某用户
     * @return 如果已关注返回true，否则返回false
     */
    @Override
    public boolean isFollowing(Integer targetUserId) {
        // 获取当前登录用户ID
        Integer uid = UserContextHolder.getUid();
        if (uid == null) {
            // 未登录用户视为未关注
            return false;
        }

        // 查询是否已关注该用户
        long count = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, uid)
                .eq(UserFollow::getFollowedId, targetUserId)
                .eq(UserFollow::getStatus, 1) // 只查询激活状态的关注
                .count();
        
        return count > 0;
    }
    
    /**
     * 获取当前用户关注的用户ID列表
     */
    @Override
    public List<Integer> getFollowingUserIds(Integer userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // 查询当前用户关注的所有用户ID (只查询激活状态的关注)
        return this.lambdaQuery()
                .select(UserFollow::getFollowedId)
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1) // 只查询激活状态的关注
                .list()
                .stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的关注统计信息
     */
    @Override
    public UserFollowStatsDto getUserFollowStats(Integer userId) {
        // 当userId为null时,如果未登录,直接抛出异常
        if (userId == null) {
            userId = UserContextHolder.getUid();
            if (userId == null) {
                throw new IllegalArgumentException("未登录且未指定用户ID，请提供要查询的用户ID");
            }
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
    @Override
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
    @Override
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
    @Override
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