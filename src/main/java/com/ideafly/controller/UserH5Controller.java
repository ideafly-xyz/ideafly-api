package com.ideafly.controller;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.user.UpdateUserInputDto;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.dto.user.UserGetOutputDto;
import com.ideafly.model.Users;
import com.ideafly.service.JobLikesService;
import com.ideafly.service.UserFollowService;
import com.ideafly.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "用户相关接口", description = "用户相关接口")
@RestController
@RequestMapping("/api/user")
public class UserH5Controller {
    @Resource
    private UsersService usersService;
    
    @Resource
    private UserFollowService userFollowService;
    
    @Resource
    private JobLikesService jobLikesService;

    @GetMapping("get")
    @Operation(summary = "获取用户信息", description = "获取用户信息")
    public R<UserGetOutputDto> getUserInfo() {
        String uid = UserContextHolder.getUid();
        if (Objects.isNull(uid)) {
            return R.error("未登录");
        }
        Users user = usersService.getById(uid);
        UserGetOutputDto userGetOutputDto = new UserGetOutputDto();
        userGetOutputDto.setId(user.getId());
        userGetOutputDto.setUsername(user.getUsername());
        userGetOutputDto.setEmail(user.getEmail());
        userGetOutputDto.setMobile(user.getMobile());
        userGetOutputDto.setAvatar(user.getAvatar());
        userGetOutputDto.setPersonalBio(user.getBio());
        userGetOutputDto.setLocation(user.getLocation());
        userGetOutputDto.setGender(user.getGender());
        
        // 从job_likes表计算总点赞数
        Integer totalLikes = jobLikesService.calculateUserTotalLikes(uid);
        userGetOutputDto.setTotalLikes(totalLikes);
        
        // 添加关注统计信息
        try {
            UserFollowStatsDto followStats = userFollowService.getUserFollowStats(uid);
            userGetOutputDto.setFollowersCount(followStats.getFollowersCount());
            userGetOutputDto.setFollowingCount(followStats.getFollowingCount());
            userGetOutputDto.setMutualFollowCount(followStats.getMutualFollowCount());
        } catch (Exception e) {
            // 如果获取关注统计失败，使用默认值0
            userGetOutputDto.setFollowersCount(0);
            userGetOutputDto.setFollowingCount(0);
            userGetOutputDto.setMutualFollowCount(0);
        }
        
        return R.success(userGetOutputDto);
    }

        // 点赞相关方法已移至 JobLikesController
    
    @NoAuth
    @GetMapping("profile/{userId}")
    @Operation(summary = "获取用户公开资料", description = "获取指定用户的公开资料，无需登录")
    public R<Map<String, Object>> getUserProfile(@PathVariable("userId") String userId) {
        Users user = usersService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("avatar", user.getAvatar());
        profile.put("bio", user.getBio());
        
        // 从job_likes表计算总点赞数
        Integer totalLikes = jobLikesService.calculateUserTotalLikes(userId);
        profile.put("totalLikes", totalLikes);
        
        // 添加性别和位置字段
        profile.put("gender", user.getGender());
        profile.put("location", user.getLocation());
        
        // 添加关注统计信息
        try {
            UserFollowStatsDto followStats = userFollowService.getUserFollowStats(userId);
            profile.put("followersCount", followStats.getFollowersCount());
            profile.put("followingCount", followStats.getFollowingCount());
            profile.put("mutualFollowCount", followStats.getMutualFollowCount());
            
            // 如果当前有登录用户，添加是否已关注该用户的信息
            String currentUserId = UserContextHolder.getUid();
            if (currentUserId != null && !currentUserId.equals(userId)) {
                boolean isFollowing = userFollowService.isFollowing(userId);
                profile.put("isFollowing", isFollowing);
            }
        } catch (Exception e) {
            // 如果获取关注统计失败，使用默认值0
            profile.put("followersCount", 0);
            profile.put("followingCount", 0);
            profile.put("mutualFollowCount", 0);
        }
        
        return R.success(profile);
    }

    @PostMapping("/update")
    public R<Boolean> update(@RequestBody UpdateUserInputDto dto){
        System.out.println("【控制器调试日志】接收到的UpdateUserInputDto: " + dto);
        System.out.println("【控制器调试日志】个人简介personalBio值: " + dto.getPersonalBio());
        usersService.updateUser(dto);
        return R.success(Boolean.TRUE);
    }
    
    @NoAuth
    @GetMapping("followStats")
    @Operation(summary = "获取用户关注统计信息", description = "获取当前登录用户或指定用户的关注统计信息")
    public R<UserFollowStatsDto> getUserFollowStats(@RequestParam(value = "userId", required = false) String userId) {
        try {
            String targetUserId = userId;
            // 如果未指定用户ID，则使用当前登录用户ID
            if (targetUserId == null) {
                targetUserId = UserContextHolder.getUid();
                if (targetUserId == null) {
                    return R.error("请提供要查询的用户ID");
                }
            }
            
            // 验证用户存在
            Users user = usersService.getById(targetUserId);
            if (user == null) {
                return R.error("用户不存在，ID: " + targetUserId);
            }
            
            UserFollowStatsDto stats = userFollowService.getUserFollowStats(targetUserId);
            return R.success(stats);
        } catch (Exception e) {
            return R.error("获取关注统计失败: " + e.getMessage());
        }
    }
}
