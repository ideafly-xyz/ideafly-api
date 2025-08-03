package com.ideafly.service.impl.users;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.dto.user.UserFollowInputDto;
import com.ideafly.dto.user.UserFollowStatusDto;
import com.ideafly.mapper.users.UserFollowMapper;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.model.users.UserFollow;
import com.ideafly.model.users.Users;
import com.ideafly.service.UserFollowService;

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
     * 关注用户
     */
    public void followUser(String userId, String targetUserId) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("被关注者ID不能为空");
        }
        
        // 不允许关注自己
        if (Objects.equals(userId, targetUserId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        
        // 检查被关注的用户是否存在
        Users followedUser = usersService.getById(targetUserId);
        if (followedUser == null) {
            throw new IllegalArgumentException("被关注的用户不存在");
        }
        
        // 查找是否已存在关注记录
        UserFollow userFollow = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowedId, targetUserId)
                .one();
        
        // 如果记录不存在，创建新的关注关系
        if (userFollow == null) {
            userFollow = new UserFollow();
            userFollow.setFollowerId(userId);
            userFollow.setFollowedId(targetUserId);
            userFollow.setCreatedAt(new Date());
            userFollow.setStatus(1); // 激活状态
            this.save(userFollow);
        } else if (userFollow.getStatus() == 0) {
            // 如果记录存在但状态为0，激活关注状态
            userFollow.setStatus(1);
            this.updateById(userFollow);
        }
    }

    /**
     * 取消关注用户
     */
    public void unfollowUser(String userId, String targetUserId) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("被关注者ID不能为空");
        }
        
        // 查找关注记录
        UserFollow userFollow = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowedId, targetUserId)
                .one();
        
        if (userFollow != null && userFollow.getStatus() == 1) {
            // 如果记录存在且状态为1，取消关注
            userFollow.setStatus(0);
            this.updateById(userFollow);
        }
    }

    /**
     * 获取粉丝用户ID列表
     */
    public List<String> getFollowersUserIds(String userId) {
        return this.lambdaQuery()
                .eq(UserFollow::getFollowedId, userId)
                .eq(UserFollow::getStatus, 1)
                .list()
                .stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());
    }

    /**
     * 判断用户是否已关注某用户
     */
    public boolean isFollowing(String userId, String targetUserId) {
        if (userId == null || targetUserId == null) {
            return false;
        }
        long count = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowedId, targetUserId)
                .eq(UserFollow::getStatus, 1)
                .count();
        return count > 0;
    }

    /**
     * 添加或取消关注
     */
    @Override
    public UserFollowStatusDto toggleFollow(UserFollowInputDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("未提供关注参数");
        }
        
        String followedId = dto.getFollowedId();
        
        if (followedId == null) {
            throw new IllegalArgumentException("被关注者ID不能为空");
        }
        
        // 构建返回对象
        UserFollowStatusDto result = new UserFollowStatusDto();
        result.setFollowedId(followedId);
        
        // 这个方法需要重构，因为DTO中没有followerId
        // 暂时保留原有逻辑，但需要传入followerId参数
        throw new UnsupportedOperationException("此方法需要重构，请使用新的followUser/unfollowUser方法");
    }

    /**
     * 判断当前用户是否已关注某用户
     * @return 如果已关注返回true，否则返回false
     */
    @Override
    public boolean isFollowing(String followedId) {
        // 这个方法需要重构，因为无法获取当前用户ID
        // 暂时保留原有逻辑，但需要传入followerId参数
        throw new UnsupportedOperationException("此方法需要重构，请使用新的isFollowing(String, String)方法");
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
        // 如果userId为null，则抛出异常提示需要userId参数
        if (userId == null) {
            throw new IllegalArgumentException("请提供要查询的用户ID");
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
            throw new IllegalArgumentException("请提供要查询的用户ID");
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
            throw new IllegalArgumentException("请提供要查询的用户ID");
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
            throw new IllegalArgumentException("请提供要查询的用户ID");
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