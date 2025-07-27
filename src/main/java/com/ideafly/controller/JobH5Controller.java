package com.ideafly.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.*;
import com.ideafly.model.Jobs;
import com.ideafly.service.impl.JobsService;
import com.ideafly.service.impl.interact.CommentService;
import com.ideafly.service.impl.interact.JobLikesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "工作相关接口", description = "工作相关接口")
@RestController
@RequestMapping("/api/jobs")
public class JobH5Controller {

    @Resource
    private JobsService jobService;
    @Resource
    private JobLikesService jobLikesService;
    @Resource
    private CommentService commentService;
    
    @NoAuth
    @GetMapping("list")
    @Operation(summary = "获取职位列表", description = "使用游标分页获取职位列表")
    public R<?> getJobList(
            @RequestParam(value = "pageSize", defaultValue = "3") Integer pageSize,
            @RequestParam(value = "maxCursor", required = false) String maxCursor,
            @RequestParam(value = "minCursor", required = false) String minCursor) {
        
        // 构建请求DTO
        JobListInputDto request = new JobListInputDto();
        request.setPageSize(pageSize);
        request.setMaxCursor(maxCursor);
        request.setMinCursor(minCursor);
        
        // 添加日志，方便调试分页问题
        System.out.println("【JobH5Controller】获取职位列表，" + 
                "每页数量: " + request.getPageSize() + 
                ", 使用游标分页" +
                ", 最大游标: " + request.getMaxCursor() +
                ", 最小游标: " + request.getMinCursor());
        
        // 直接调用游标分页方法
        CursorResponseDto<JobDetailOutputDto> result = jobService.getJobsWithCursor(request);
        
        // 记录游标分页结果
        System.out.println("【JobH5Controller】获取职位列表结果(游标分页)，记录数: " + result.getRecords().size() + 
                ", 下一个maxCursor: " + result.getNextMaxCursor() +
                ", 下一个minCursor: " + result.getNextMinCursor() +
                ", 是否有更多历史内容: " + result.getHasMoreHistory() +
                ", 是否有更多新内容: " + result.getHasMoreNew());
        
        return R.success(result);
    }
    
    /**
     * 获取当前用户自己发布的职位列表 (仅支持游标分页)
     */
    @PostMapping("myPosts")
    @Operation(summary = "获取我的作品", description = "获取当前用户发布的所有作品，使用游标分页")
    public R<?> getMyPosts(@RequestBody JobListInputDto request) {
        // 获取当前用户ID
        String userId = UserContextHolder.getUid();
        if (userId == null) {
            return R.error("用户未登录");
        }
        
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
}