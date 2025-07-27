package com.ideafly.controller.interact;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.interact.JobLikeInputDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.service.impl.interact.JobLikesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "点赞相关接口", description = "点赞相关功能接口")
@RestController
@RequestMapping("/api/jobs/likes")
public class PostLikesController {

    @Resource
    private JobLikesService jobLikesService;

    /**
     * 获取用户点赞的职位列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取点赞职位", description = "获取当前用户点赞的所有职位")
    public R<Page<JobDetailOutputDto>> getLikedJobs(@RequestBody JobListInputDto request) {
        return R.success(jobLikesService.getUserLikedJobs(request));
    }

    /**
     * 点赞或取消点赞
     */
    @PostMapping("/toggle")
    @Operation(summary = "点赞", description = "点赞或取消点赞功能")
    public R<Boolean> toggleLike(@RequestBody @Valid JobLikeInputDto request) {
        System.out.println("收到点赞请求 - JobID: " + request.getJobId() + ", isLike: " + request.getIsLike() + ", 用户ID: " + UserContextHolder.getUid());
        try {
            jobLikesService.addOrRemoveLike(request);
            System.out.println("点赞操作成功 - JobID: " + request.getJobId() + ", isLike: " + request.getIsLike());
            return R.success(Boolean.TRUE);
        } catch (Exception e) {
            System.out.println("点赞操作失败 - " + e.getMessage());
            e.printStackTrace();
            return R.error("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户获得的总点赞数
     */
    @NoAuth
    @GetMapping("/count")
    @Operation(summary = "获取用户总点赞数", description = "获取指定用户获得的总点赞数")
    public R<Map<String, Object>> getUserTotalLikes(@RequestParam(value = "userId", required = false) String userId) {
        long startTime = System.currentTimeMillis();
        System.out.println("====== 获取用户总点赞数 API 开始处理 ======");
        System.out.println("请求参数: userId=" + userId);
        String targetUserId = userId;
        // 如果未指定用户ID，则使用当前登录用户ID
        if (targetUserId == null) {
            targetUserId = UserContextHolder.getUid();
            System.out.println("使用当前登录用户ID: " + targetUserId);
            if (targetUserId == null) {
                System.out.println("错误: 未登录且未指定用户ID");
                return R.error("请提供要查询的用户ID");
            }
        }
        // 计算用户获得的总点赞数
        Integer totalLikes = jobLikesService.calculateUserTotalLikes(targetUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", targetUserId);
        result.put("totalLikes", totalLikes);
        long endTime = System.currentTimeMillis();
        System.out.println("总点赞数: " + totalLikes);
        System.out.println("API总处理耗时: " + (endTime - startTime) + "ms");
        System.out.println("====== 获取用户总点赞数 API 处理完成 ======");
        return R.success(result);
    }
} 