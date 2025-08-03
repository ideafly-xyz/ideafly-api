package com.ideafly.controller.users;

import com.ideafly.common.R;
import com.ideafly.common.RequestUtils;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.service.UserFollowService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Tag(name = "用户关注相关接口", description = "用户关注相关功能接口")
@RestController
@RequestMapping("/api/users/follows")
@Slf4j
public class UsersFollowController {

    @Resource
    private UserFollowService userFollowService;

    /**
     * 关注用户
     */
    @PostMapping("/follow")
    @Operation(summary = "关注用户", description = "关注指定用户")
    public R<Boolean> followUser(@RequestParam("targetUserId") String targetUserId, HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        if (userId == null) {
            return R.error("用户未登录");
        }
        
        log.info("当前用户ID: {}", userId);
        userFollowService.followUser(userId, targetUserId);
        return R.success(Boolean.TRUE);
    }

    /**
     * 取消关注用户
     */
    @PostMapping("/unfollow")
    @Operation(summary = "取消关注", description = "取消关注指定用户")
    public R<Boolean> unfollowUser(@RequestParam("targetUserId") String targetUserId, HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        if (userId == null) {
            return R.error("用户未登录");
        }
        
        userFollowService.unfollowUser(userId, targetUserId);
        return R.success(Boolean.TRUE);
    }

    /**
     * 获取关注列表
     */
    @GetMapping("/following")
    @Operation(summary = "获取关注列表", description = "获取当前用户关注的用户列表")
    public R<List<String>> getFollowingList(HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        if (userId == null) {
            return R.error("用户未登录");
        }
        
        List<String> followingList = userFollowService.getFollowingUserIds(userId);
        return R.success(followingList);
    }

    /**
     * 获取粉丝列表
     */
    @GetMapping("/followers")
    @Operation(summary = "获取粉丝列表", description = "获取当前用户的粉丝列表")
    public R<List<String>> getFollowersList(HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        if (userId == null) {
            return R.error("用户未登录");
        }
        
        List<String> followersList = userFollowService.getFollowersUserIds(userId);
        return R.success(followersList);
    }

    /**
     * 获取关注统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取关注统计", description = "获取当前用户的关注统计信息")
    public R<UserFollowStatsDto> getFollowStats(HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        if (userId == null) {
            return R.error("用户未登录");
        }
        
        UserFollowStatsDto stats = userFollowService.getUserFollowStats(userId);
        return R.success(stats);
    }

    /**
     * 检查是否关注
     */
    @GetMapping("/check")
    @Operation(summary = "检查关注状态", description = "检查当前用户是否关注了指定用户")
    public R<Boolean> checkFollowStatus(@RequestParam("targetUserId") String targetUserId, HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        if (userId == null) {
            return R.error("用户未登录");
        }
        
        boolean isFollowing = userFollowService.isFollowing(userId, targetUserId);
        return R.success(isFollowing);
    }
} 