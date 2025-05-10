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
        
        // 设置默认页大小
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(3);
        }
        
        // 添加日志，方便调试分页问题
        System.out.println("【JobH5Controller】获取职位列表，" + 
                "每页数量: " + request.getPageSize() + 
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
            @RequestParam(value = "pageSize", defaultValue = "3") Integer pageSize,
            @RequestParam(value = "useCursor", required = false) Boolean useCursor,
            @RequestParam(value = "maxCursor", required = false) String maxCursor,
            @RequestParam(value = "minCursor", required = false) String minCursor) {
        
        // 构建请求DTO
        JobListInputDto request = new JobListInputDto();
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
    public R<?> getFavoriteJobs(@RequestBody JobListInputDto request) {
        // 确保请求参数合法
        if (request == null) {
            request = new JobListInputDto();
        }
        
        // 设置默认页大小
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(3);
        }
        
        // 添加日志，方便调试分页问题
        System.out.println("【JobH5Controller】获取收藏职位列表，" + 
                "每页数量: " + request.getPageSize() + 
                ", 使用游标: " + (Boolean.TRUE.equals(request.getUseCursor()) ? "是" : "否") +
                ", 最大游标: " + request.getMaxCursor() +
                ", 最小游标: " + request.getMinCursor());
        
        // 调用服务获取结果
        Object result = jobFavoriteService.getUserFavoriteJobs(request);
        
        // 根据返回类型进行不同的日志记录
        if (result instanceof Page) {
            Page<JobDetailOutputDto> pageResult = (Page<JobDetailOutputDto>) result;
            System.out.println("【JobH5Controller】获取收藏职位列表结果(传统分页)，当前页: " + pageResult.getCurrent() + 
                    ", 总页数: " + pageResult.getPages() + 
                    ", 总记录数: " + pageResult.getTotal() + 
                    ", 本页记录数: " + pageResult.getRecords().size());
        } else if (result instanceof CursorResponseDto) {
            CursorResponseDto<JobDetailOutputDto> cursorResult = (CursorResponseDto<JobDetailOutputDto>) result;
            System.out.println("【JobH5Controller】获取收藏职位列表结果(游标分页)，记录数: " + cursorResult.getRecords().size() + 
                    ", 下一个maxCursor: " + cursorResult.getNextMaxCursor() +
                    ", 下一个minCursor: " + cursorResult.getNextMinCursor() +
                    ", 是否有更多历史内容: " + cursorResult.getHasMoreHistory() +
                    ", 是否有更多新内容: " + cursorResult.getHasMoreNew());
        }
        
        return R.success(result);
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
    @Operation(summary = "获取我的作品", description = "获取当前用户发布的所有作品，强制使用游标分页")
    public R<?> getMyPosts(@RequestBody JobListInputDto request) {
        // 获取当前用户ID
        Integer userId = UserContextHolder.getUid();
        if (userId == null) {
            return R.error("用户未登录");
        }
        
        // 确保使用游标分页
        request.setUseCursor(true);
        
        // 确保页大小合理，如果客户端未指定或值不合理则使用默认值
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(4); // 默认每页加载4条作品
        }
        
        // 打印详细请求日志，用于调试
        System.out.println("【JobH5Controller】获取用户作品列表详细参数:");
        System.out.println("  - 用户ID: " + userId);
        System.out.println("  - 页大小: " + request.getPageSize());
        System.out.println("  - maxCursor: " + request.getMaxCursor());
        System.out.println("  - minCursor: " + request.getMinCursor());
        System.out.println("  - 请求头: " + request);
        
        // 检查游标值是否有效
        if (request.getMaxCursor() != null) {
            // 验证maxCursor格式是否正确
            try {
                String cursor = request.getMaxCursor();
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(cursor);
                String jsonStr = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("【游标检查】maxCursor解码后: " + jsonStr);
            } catch (Exception e) {
                System.out.println("【警告】无效的maxCursor格式: " + request.getMaxCursor() + ", 错误: " + e.getMessage());
                // 如果解析失败，清除游标值避免后续查询出错
                request.setMaxCursor(null);
            }
        } else {
            System.out.println("【警告】maxCursor为null，如果是首次加载则正常，如果是上拉加载更多则表示客户端未正确传递上一次的nextMaxCursor");
        }
        
        // 获取用户发布的职位（专用游标分页方法）
        Object result = jobService.getUserPosts(request, userId);
        
        // 打印响应日志
        if (result instanceof CursorResponseDto) {
            CursorResponseDto<?> cursorResponse = (CursorResponseDto<?>) result;
            System.out.println("【JobH5Controller】获取用户作品列表结果:");
            System.out.println("  - 记录数: " + cursorResponse.getRecords().size());
            System.out.println("  - 下一个maxCursor: " + cursorResponse.getNextMaxCursor());
            System.out.println("  - 下一个minCursor: " + cursorResponse.getNextMinCursor());
            System.out.println("  - 是否有更多历史: " + cursorResponse.getHasMoreHistory());
            System.out.println("  - 是否有更多新内容: " + cursorResponse.getHasMoreNew());
            
            // 检查返回的nextMaxCursor是否为null或为空
            if (cursorResponse.getNextMaxCursor() == null || cursorResponse.getNextMaxCursor().isEmpty()) {
                System.out.println("【严重警告】返回的nextMaxCursor为null或空，这将导致前端无法正确加载下一页数据");
            }
        }
        
        return R.success(result);
    }
    
    /**
     * 获取关注用户发布的帖子列表
     */
    @GetMapping("following")
    @Operation(summary = "获取关注用户帖子", description = "获取当前用户关注的人发布的帖子")
    public R<Page<JobDetailOutputDto>> getFollowingUserJobs(
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        
        // 创建请求DTO
        JobListInputDto request = new JobListInputDto();
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
    @PostMapping("commentsByCursor")
    @Operation(summary = "获取评论列表(游标分页)", description = "使用游标分页获取评论列表")
    public R<JobCommentCursorDto> getCommentsByCursor(@RequestBody JobCommentPageDto request) {
        return R.success(jobCommentsService.getCommentsByCursor(request));
    }

    @NoAuth
    @GetMapping("commentsCount")
    public R<Integer> getCommentsCount(@RequestParam(name = "job_id") Integer jobId) {
        return R.success(jobCommentsService.getJobCommentsCount(jobId));
    }
}