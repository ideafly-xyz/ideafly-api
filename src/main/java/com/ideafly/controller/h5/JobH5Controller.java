package com.ideafly.controller.h5;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.*;
import com.ideafly.model.JobComments;
import com.ideafly.model.Jobs;
import com.ideafly.service.JobCommentsService;
import com.ideafly.service.JobFavoriteService;
import com.ideafly.service.JobLikesService;
import com.ideafly.service.JobsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Tag(name = "工作相关接口", description = "工作相关接口")
@RestController
@RequestMapping("/api/jobs")
public class JobH5Controller {

    @Resource
    private JobsService jobService;
    @Resource
    private JobLikesService jobLikesService;
    @Resource
    private JobFavoriteService jobFavoriteService;
    @Resource
    private JobCommentsService jobCommentsService;
    
    @NoAuth
    @PostMapping("list")
    public R<Page<JobDetailOutputDto>> getJobList(@RequestBody JobListInputDto request) {
        return R.success(jobService.getJobList(request));
    }
    
    /**
     * 获取用户收藏的职位列表
     */
    @PostMapping("favorites")
    @Operation(summary = "获取收藏职位", description = "获取当前用户收藏的所有职位")
    public R<Page<JobDetailOutputDto>> getFavoriteJobs(@RequestBody JobListInputDto request) {
        return R.success(jobFavoriteService.getUserFavoriteJobs(request));
    }
    
    /**
     * 获取用户点赞的职位列表
     */
    @PostMapping("likes")
    @Operation(summary = "获取点赞职位", description = "获取当前用户点赞的所有职位")
    public R<Page<JobDetailOutputDto>> getLikedJobs(@RequestBody JobListInputDto request) {
        return R.success(jobLikesService.getUserLikedJobs(request));
    }
    
    /**
     * 发布职位接口
     */
    @PostMapping("createJob")
    public R<Boolean> createJob(@Valid @RequestBody CreateJobInputDto request) { //  使用 @Valid 注解开启参数校验
        Jobs job = jobService.createJob(request);
        return R.success(Boolean.TRUE);
    }
    @PostMapping("/like")
    @Operation(summary = "点赞", description = "点赞功能")
    public R<Boolean> like(@RequestBody @Valid JobLikeInputDto request) {
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
    @PostMapping("favorite")
    public R<Boolean> favorite(@Valid @RequestBody JobFavoriteInputDto request) { //  使用 @Valid 注解开启参数校验
        jobFavoriteService.addOrRemoveFavorite(request);
        return R.success(Boolean.TRUE);
    }
    @PostMapping("comment")
    public R<Boolean> comment(@Valid @RequestBody JobCommentInputDto request) { //  使用 @Valid 注解开启参数校验
        jobCommentsService.addComment(request);
        return R.success(Boolean.TRUE);
    }
    @NoAuth
    @GetMapping("getComment")
    public R<List<JobComments>> getComment(@RequestParam(name = "job_id") Integer jobId) { //  使用 @Valid 注解开启参数校验
        return R.success(jobCommentsService.getCommentTreeByJobId(jobId));
    }
    
    @NoAuth
    @GetMapping("commentsCount")
    public R<Integer> getCommentsCount(@RequestParam(name = "job_id") Integer jobId) {
        return R.success(jobCommentsService.getJobCommentsCount(jobId));
    }
}