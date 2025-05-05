package com.ideafly.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dao.UserFollowMapper;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.model.UserFollow;
import com.ideafly.model.Users;
import com.ideafly.service.UserFollowService;
import com.ideafly.service.UsersService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements UserFollowService {

    @Resource
    private UsersService usersService;

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
} 