package com.ideafly.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.service.JobFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "收藏相关接口", description = "收藏相关功能接口")
@RestController
@RequestMapping("/api/jobs/favorites")
public class PostFavoritesController {

    @Resource
    private JobFavoriteService jobFavoriteService;

    /**
     * 获取用户收藏的职位列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取收藏职位", description = "获取当前用户收藏的所有职位")
    public R<?> getFavoriteJobs(@RequestBody JobListInputDto request) {
        return R.success(jobFavoriteService.getUserFavoriteJobs(request));
    }

    /**
     * 收藏或取消收藏
     */
    @PostMapping("/toggle")
    @Operation(summary = "收藏", description = "收藏或取消收藏功能")
    public R<Boolean> toggleFavorite(@RequestBody @Valid JobFavoriteInputDto request) {
        System.out.println("收到收藏请求 - JobID: " + request.getJobId() + ", isFavorite: " + request.getIsFavorite() + ", 用户ID: " + UserContextHolder.getUid());
        try {
            jobFavoriteService.addOrRemoveFavorite(request);
            System.out.println("收藏操作成功 - JobID: " + request.getJobId() + ", isFavorite: " + request.getIsFavorite());
            return R.success(Boolean.TRUE);
        } catch (Exception e) {
            System.out.println("收藏操作失败 - " + e.getMessage());
            e.printStackTrace();
            return R.error("收藏失败: " + e.getMessage());
        }
    }
} 