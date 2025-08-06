package com.ideafly.controller.interact;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.common.RequestUtils;
import com.ideafly.dto.interact.JobLikeInputDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.service.impl.interact.JobLikesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "点赞相关接口", description = "点赞相关功能接口")
@RestController
@RequestMapping("/api/jobs/likes")
@Slf4j
public class PostLikesController {

    @Resource
    private JobLikesService jobLikesService;

    /**
     * 点赞或取消点赞
     */
    @PostMapping("/toggle")
    @Operation(summary = "点赞", description = "点赞或取消点赞功能")
    public R<Map<String, Object>> toggleLike(@RequestBody @Valid JobLikeInputDto request, HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        log.info("收到点赞请求 - JobID: {}, isLike: {}, 用户ID: {}", request.getJobId(), request.getIsLike(), userId);
        try {
            jobLikesService.addOrRemoveLike(request, userId);
            boolean liked = jobLikesService.isJobLike(request.getJobId(), userId);
            int likeCount = jobLikesService.getJobLikesCount(request.getJobId());
            log.info("点赞操作成功 - JobID: {}, isLike: {}, 当前点赞状态: {}, 总点赞数: {}", request.getJobId(), request.getIsLike(), liked, likeCount);
            Map<String, Object> result = new HashMap<>();
            result.put("jobId", request.getJobId());
            result.put("liked", liked);
            result.put("likeCount", likeCount);
            return R.success(result);
        } catch (Exception e) {
            log.error("点赞操作失败 - {}", e.getMessage(), e);
            return R.error("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前用户对职位的点赞详情（是否点赞+总数）
     */
    @GetMapping("/info")
    @Operation(summary = "查询职位点赞详情", description = "返回当前用户是否点赞及职位点赞总数")
    public R<Map<String, Object>> getLikeInfo(@RequestParam("jobId") Integer jobId, HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        boolean liked = false;
        if (userId != null) {
            liked = jobLikesService.isJobLike(jobId, userId);
        }
        int likeCount = jobLikesService.getJobLikesCount(jobId);
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobId);
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        return R.success(result);
    }

    /**
     * 获取用户获得的总点赞数
     */
    @NoAuth
    @GetMapping("/count")
    @Operation(summary = "获取用户总点赞数", description = "获取指定用户获得的总点赞数")
    public R<Map<String, Object>> getUserTotalLikes(@RequestParam(value = "userId", required = false) String userId, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        log.info("====== 获取用户总点赞数 API 开始处理 ======");
        log.info("请求参数: userId={}", userId);
        String targetUserId = userId;
        // 如果未指定用户ID，则使用当前登录用户ID
        if (targetUserId == null) {
            targetUserId = RequestUtils.getCurrentUserId(httpRequest);
            log.info("使用当前登录用户ID: {}", targetUserId);
            if (targetUserId == null) {
                log.warn("错误: 未登录且未指定用户ID");
                return R.error("请提供要查询的用户ID");
            }
        }
        // 计算用户获得的总点赞数
        Integer totalLikes = jobLikesService.calculateUserTotalLikes(targetUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", targetUserId);
        result.put("totalLikes", totalLikes);
        long endTime = System.currentTimeMillis();
        log.info("总点赞数: {}", totalLikes);
        log.info("API总处理耗时: {}ms", (endTime - startTime));
        log.info("====== 获取用户总点赞数 API 处理完成 ======");
        return R.success(result);
    }

    /**
     * 获取用户点赞的职位列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取点赞职位", description = "获取当前用户点赞的所有职位")
    public R<Page<JobDetailOutputDto>> getLikedJobs(@RequestBody JobListInputDto request, HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        return R.success(jobLikesService.getUserLikedJobs(request, userId));
    }

} 