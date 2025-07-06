package com.ideafly.controller;

import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.user.UserFollowInputDto;
import com.ideafly.dto.user.UserFollowStatusDto;
import com.ideafly.dto.user.UserFollowStatsDto;
import com.ideafly.model.Users;
import com.ideafly.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户关注相关API接口
 */
@Tag(name = "用户关注相关接口", description = "包含关注/取消关注、获取关注统计等功能")
@RestController
@RequestMapping("/api/user/follow")
@Slf4j
public class UserFollowController {

    @Resource
    private UserFollowService userFollowService;

    @PostMapping("toggle")
    @Operation(summary = "关注或取消关注用户", description = "切换对指定用户的关注状态")
    public R<UserFollowStatusDto> toggleFollow(@RequestBody UserFollowInputDto dto) {
        log.info("=== 关注/取消关注请求开始 ===");
        log.info("接收到的参数: {}", dto);
        
        if (dto == null) {
            log.error("接收到空对象");
        } else {
            log.info("followedId: {}", dto.getFollowedId());
        }
        
        // 打印请求相关详细信息，帮助调试
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            log.info("Content-Type: {}", request.getContentType());
            log.info("Character Encoding: {}", request.getCharacterEncoding());
            log.info("Content Length: {}", request.getContentLength());
            log.info("HTTP Method: {}", request.getMethod());
            
            // 打印头部信息
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.info("Header: {} = {}", headerName, request.getHeader(headerName));
            }
        } catch (Exception e) {
            log.error("获取请求信息异常: {}", e.getMessage());
        }
        
        log.info("当前用户ID: {}", UserContextHolder.getUid());
        
        try {
            UserFollowStatusDto result = userFollowService.toggleFollow(dto);
            log.info("关注/取消关注执行结果: {}", result);
            return R.success(result);
        } catch (IllegalArgumentException e) {
            log.error("关注/取消关注参数错误: {}", e.getMessage());
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("关注/取消关注异常: {}", e.getMessage(), e);
            return R.error("操作失败: " + e.getMessage());
        }
    }

    @GetMapping("check/{userId}")
    @Operation(summary = "检查是否关注了某用户", description = "检查当前登录用户是否已关注指定用户")
    public R<Map<String, Object>> checkFollow(
            @Parameter(description = "被检查的用户ID") @PathVariable("userId") String userId) {
        try {
            boolean isFollowing = userFollowService.isFollowing(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("isFollowing", isFollowing);
            result.put("userId", userId);
            return R.success(result);
        } catch (Exception e) {
            return R.error("检查关注状态失败: " + e.getMessage());
        }
    }

    @GetMapping("stats")
    @Operation(summary = "获取用户关注统计信息", description = "获取指定用户的关注、粉丝和互关数量")
    public R<UserFollowStatsDto> getUserFollowStats(
            @Parameter(description = "用户ID，不传默认为当前登录用户") @RequestParam(required = false) String userId) {
        try {
            UserFollowStatsDto stats = userFollowService.getUserFollowStats(userId);
            return R.success(stats);
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            return R.error("获取关注统计失败: " + e.getMessage());
        }
    }

    @GetMapping("following")
    @Operation(summary = "获取用户关注列表", description = "获取指定用户关注的人列表")
    public R<List<Map<String, Object>>> getUserFollowing(
            @Parameter(description = "用户ID，不传默认为当前登录用户") @RequestParam(required = false) String userId) {
        try {
            List<Users> followingList = userFollowService.getUserFollowing(userId);
            List<Map<String, Object>> result = followingList.stream().map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("username", user.getUsername());
                map.put("avatar", user.getAvatar());
                map.put("bio", user.getBio());
                return map;
            }).collect(Collectors.toList());
            return R.success(result);
        } catch (Exception e) {
            return R.error("获取关注列表失败: " + e.getMessage());
        }
    }

    @GetMapping("followers")
    @Operation(summary = "获取用户粉丝列表", description = "获取关注指定用户的人列表")
    public R<List<Map<String, Object>>> getUserFollowers(
            @Parameter(description = "用户ID，不传默认为当前登录用户") @RequestParam(required = false) String userId) {
        try {
            List<Users> followersList = userFollowService.getUserFollowers(userId);
            List<Map<String, Object>> result = followersList.stream().map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("username", user.getUsername());
                map.put("avatar", user.getAvatar());
                map.put("bio", user.getBio());
                return map;
            }).collect(Collectors.toList());
            return R.success(result);
        } catch (Exception e) {
            return R.error("获取粉丝列表失败: " + e.getMessage());
        }
    }

    @GetMapping("mutual")
    @Operation(summary = "获取互相关注的用户列表", description = "获取与指定用户互相关注的用户列表")
    public R<List<Map<String, Object>>> getMutualFollows(
            @Parameter(description = "用户ID，不传默认为当前登录用户") @RequestParam(required = false) String userId) {
        try {
            List<Users> mutualList = userFollowService.getMutualFollows(userId);
            List<Map<String, Object>> result = mutualList.stream().map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("username", user.getUsername());
                map.put("avatar", user.getAvatar());
                map.put("bio", user.getBio());
                return map;
            }).collect(Collectors.toList());
            return R.success(result);
        } catch (Exception e) {
            return R.error("获取互关列表失败: " + e.getMessage());
        }
    }
} 