package com.ideafly.controller;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.dto.job.JobCommentCursorDto;
import com.ideafly.dto.job.JobCommentInputDto;
import com.ideafly.dto.job.JobCommentPageDto;
import com.ideafly.dto.job.JobLoadMoreChildrenDto;
import com.ideafly.model.JobComments;
import com.ideafly.service.JobCommentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "评论相关接口", description = "评论相关接口")
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Resource
    private JobCommentsService jobCommentsService;

    @PostMapping("add")
    @Operation(summary = "添加评论", description = "添加评论到职位")
    public R<Boolean> comment(@Valid @RequestBody JobCommentInputDto request) {
        jobCommentsService.addComment(request);
        return R.success(Boolean.TRUE);
    }
    
    @NoAuth
    @PostMapping("list")
    @Operation(summary = "获取评论列表(游标分页)", description = "使用游标分页获取评论列表")
    public R<JobCommentCursorDto> getCommentsByCursor(@RequestBody JobCommentPageDto request) {
        System.out.println("===== 评论列表请求 =====");
        System.out.println("请求参数: " + request);
        
        JobCommentCursorDto result = jobCommentsService.getCommentsByCursor(request);
        
        System.out.println("===== 评论列表响应 =====");
        System.out.println("响应结果: 评论数=" + (result.getRecords() != null ? result.getRecords().size() : 0) + 
                           ", nextCursor=" + result.getNextCursor() + 
                           ", hasMore=" + result.getHasMore());
        
        return R.success(result);
    }
    
    @NoAuth
    @PostMapping("loadMoreChildren")
    @Operation(summary = "加载更多子评论", description = "根据父评论ID加载更多子评论")
    public R<Map<String, Object>> loadMoreChildComments(@RequestBody JobLoadMoreChildrenDto request) {
        System.out.println("===== 加载更多子评论请求 =====");
        System.out.println("请求参数: jobId=" + request.getJobId() + 
                          ", parentId=" + request.getParentId() + 
                          ", cursor=" + request.getCursor());
        
        // 调用服务方法加载更多子评论
        Map<String, Object> result = jobCommentsService.loadMoreChildComments(
                request.getJobId(), 
                request.getParentId(), 
                request.getCursor()
        );
        
        System.out.println("===== 加载更多子评论响应 =====");
        System.out.println("响应结果: 子评论数=" + 
                          (result.get("children") != null ? ((List<?>) result.get("children")).size() : 0) + 
                          ", nextCursor=" + result.get("next_cursor") + 
                          ", hasMore=" + result.get("has_more_children"));
        
        return R.success(result);
    }
    
    @NoAuth
    @GetMapping("count")
    @Operation(summary = "获取评论数量", description = "获取职位评论数量")
    public R<Integer> getCommentsCount(@RequestParam("job_id") Integer jobId) {
        return R.success(jobCommentsService.getJobCommentsCount(jobId));
    }
} 