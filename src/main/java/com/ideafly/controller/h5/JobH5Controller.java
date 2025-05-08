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
    @Operation(summary = "获取职位列表", description = "支持传统分页和基于游标的分页")
    public R<?> getJobList(@RequestBody JobListInputDto request) {
        // 确保请求参数合法
        if (request == null) {
            request = new JobListInputDto();
        }
        
        // 设置默认值
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(3);
        }
        
        // 添加日志，方便调试分页问题
        System.out.println("【JobH5Controller】获取职位列表，页码: " + request.getPageNum() + 
                ", 每页数量: " + request.getPageSize() + 
                ", 使用游标: " + (Boolean.TRUE.equals(request.getUseCursor()) ? "是" : "否") +
                ", 最大游标: " + request.getMaxCursor() +
                ", 最小游标: " + request.getMinCursor());
        
        // 调用服务获取结果
        Object result = jobService.getJobList(request);
        
        // 根据返回类型进行不同的日志记录
        if (result instanceof Page) {
            Page<JobDetailOutputDto> pageResult = (Page<JobDetailOutputDto>) result;
            System.out.println("【JobH5Controller】获取职位列表结果(传统分页)，当前页: " + pageResult.getCurrent() + 
                    ", 总页数: " + pageResult.getPages() + 
                    ", 总记录数: " + pageResult.getTotal() + 
                    ", 本页记录数: " + pageResult.getRecords().size());
        } else if (result instanceof CursorResponseDto) {
            CursorResponseDto<JobDetailOutputDto> cursorResult = (CursorResponseDto<JobDetailOutputDto>) result;
            System.out.println("【JobH5Controller】获取职位列表结果(游标分页)，记录数: " + cursorResult.getRecords().size() + 
                    ", 下一个maxCursor: " + cursorResult.getNextMaxCursor() +
                    ", 下一个minCursor: " + cursorResult.getNextMinCursor() +
                    ", 是否有更多历史内容: " + cursorResult.getHasMoreHistory() +
                    ", 是否有更多新内容: " + cursorResult.getHasMoreNew());
        }
        
        return R.success(result);
    }
    
    // 添加GET请求支持
    @NoAuth
    @GetMapping("list")
    @Operation(summary = "获取职位列表(GET)", description = "通过GET请求获取职位列表，支持传统分页")
    public R<?> getJobListByGet(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "3") Integer pageSize,
            @RequestParam(value = "useCursor", required = false) Boolean useCursor,
            @RequestParam(value = "maxCursor", required = false) String maxCursor,
            @RequestParam(value = "minCursor", required = false) String minCursor) {
        
        // 构建请求DTO
        JobListInputDto request = new JobListInputDto();
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        request.setUseCursor(useCursor);
        request.setMaxCursor(maxCursor);
        request.setMinCursor(minCursor);
        
        // 调用已有方法处理
        return getJobList(request);
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
     * 获取当前用户自己发布的职位列表 (仅支持游标分页)
     */
    @PostMapping("myPosts")
    @Operation(summary = "获取我的作品", description = "获取当前用户发布的所有作品，使用游标分页")
    public R<?> getMyPosts(@RequestBody JobListInputDto request) {
        // 确保请求参数合法
        if (request == null) {
            request = new JobListInputDto();
        }
        
        // 强制使用游标分页
        request.setUseCursor(true);
        
        // 设置默认值
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        
        // 仅在客户端未指定页面大小时设置默认值
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(4); // 默认每页4个作品
        }
        
        // 获取当前用户ID
        Integer currentUserId = UserContextHolder.getUid();
        if (currentUserId == null) {
            return R.error("用户未登录");
        }
        
        // 添加请求日志
        System.out.println("【JobH5Controller】获取用户作品列表，用户ID: " + currentUserId + 
                ", 页大小: " + request.getPageSize() + 
                ", 最大游标: " + request.getMaxCursor() +
                ", 最小游标: " + request.getMinCursor());
        
        // 调用服务获取结果
        Object result = jobService.getUserPosts(request, currentUserId);
        
        // 添加结果日志
        if (result instanceof CursorResponseDto) {
            CursorResponseDto<?> cursorResult = (CursorResponseDto<?>) result;
            System.out.println("【JobH5Controller】获取用户作品列表结果，记录数: " + cursorResult.getRecords().size() + 
                    ", 下一个maxCursor: " + cursorResult.getNextMaxCursor() +
                    ", 下一个minCursor: " + cursorResult.getNextMinCursor() +
                    ", 是否有更多历史内容: " + cursorResult.getHasMoreHistory() +
                    ", 是否有更多新内容: " + cursorResult.getHasMoreNew());
        }
        
        return R.success(result);
    }
    
    /**
     * 获取关注用户发布的帖子列表
     */
    @GetMapping("following")
    @Operation(summary = "获取关注用户帖子", description = "获取当前用户关注的人发布的帖子")
    public R<Page<JobDetailOutputDto>> getFollowingUserJobs(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        
        // 创建请求DTO
        JobListInputDto request = new JobListInputDto();
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        
        // 调用新的服务方法获取关注用户的帖子
        return R.success(jobService.getFollowingUserJobs(request));
    }
    
    /**
     * 发布职位接口
     */
    @PostMapping("createJob")
    public R<JobDetailOutputDto> createJob(@Valid @RequestBody CreateJobInputDto request) { //  使用 @Valid 注解开启参数校验
        Jobs job = jobService.createJob(request);
        // 转换为包含完整信息的DTO
        JobDetailOutputDto jobDto = jobService.convertDto(job);
        return R.success(jobDto);
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