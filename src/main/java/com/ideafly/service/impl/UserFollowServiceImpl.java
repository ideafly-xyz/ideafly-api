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

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        
        String followerId = UserContextHolder.getUid();
        String followedId = dto.getFollowedId();
        
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
    public boolean isFollowing(String followedId) {
        String uid = UserContextHolder.getUid();
        if (uid == null) {
            return false;
        }
        long count = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, uid)
                .eq(UserFollow::getFollowedId, followedId)
                .eq(UserFollow::getStatus, 1)
                .count();
        return count > 0;
    }
    
    /**
     * 获取当前用户关注的用户ID列表
     */
    @Override
    public List<String> getFollowingUserIds(String userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return this.lambdaQuery()
                .select(UserFollow::getFollowedId)
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的关注统计信息
     */
    @Override
    public UserFollowStatsDto getUserFollowStats(String userId) {
        // 当userId为null时，尝试从认证信息获取用户ID
        // 如果认证信息也没有，则抛出异常提示需要userId参数
        if (userId == null) {
            userId = UserContextHolder.getUid().toString();
            if (userId == null) {
                throw new IllegalArgumentException("请提供要查询的用户ID");
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
        List<String> followingIds = this.lambdaQuery()
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
    public List<Users> getUserFollowing(String userId) {
        if (userId == null) {
            userId = UserContextHolder.getUid();
        }
        List<String> followingIds = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        return usersService.listByIds(followingIds);
    }
    
    /**
     * 获取用户的粉丝列表 (关注该用户的人)
     */
    @Override
    public List<Users> getUserFollowers(String userId) {
        if (userId == null) {
            userId = UserContextHolder.getUid();
        }
        List<String> followerIds = this.lambdaQuery()
                .eq(UserFollow::getFollowedId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());
        if (followerIds.isEmpty()) {
            return new ArrayList<>();
        }
        return usersService.listByIds(followerIds);
    }
    
    /**
     * 获取互相关注的用户列表
     */
    @Override
    public List<Users> getMutualFollows(String userId) {
        if (userId == null) {
            userId = UserContextHolder.getUid();
        }
        List<String> followingIds = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toList());
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> mutualIds = this.lambdaQuery()
                .eq(UserFollow::getFollowedId, userId)
                .in(UserFollow::getFollowerId, followingIds)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());
        if (mutualIds.isEmpty()) {
            return new ArrayList<>();
        }
        return usersService.listByIds(mutualIds);
    }
}