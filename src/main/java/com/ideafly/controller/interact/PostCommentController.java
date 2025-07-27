package com.ideafly.controller.interact;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.dto.interact.ChildCommentCursorDto;
import com.ideafly.dto.interact.JobCommentInputDto;
import com.ideafly.dto.interact.JobCommentPageDto;
import com.ideafly.dto.interact.ParentCommentCursorDto;
import com.ideafly.dto.job.*;
import com.ideafly.service.impl.interact.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "评论相关接口", description = "评论相关接口")
@RestController
@RequestMapping("/api/comments")
public class PostCommentController {

    @Resource
    private CommentService commentService;

    @PostMapping("add")
    @Operation(summary = "添加评论", description = "添加评论到职位")
    public R<Map<String, Object>> comment(@Valid @RequestBody JobCommentInputDto request) {
        Integer commentId = commentService.addComment(request);
        Map<String, Object> result = new HashMap<>();
        result.put("id", commentId);
        return R.success(result);
    }
    
    @NoAuth
    @PostMapping("list")
    @Operation(summary = "获取父评论列表(游标分页)", description = "使用游标分页获取父评论列表")
    public R<ParentCommentCursorDto> getParentCommentsByCursor(@RequestBody JobCommentPageDto request) {
        System.out.println("===== 父评论列表请求 =====");
        System.out.println("请求参数: " + request);
        
        ParentCommentCursorDto result = commentService.getParentCommentsByCursor(request);
        
        System.out.println("===== 父评论列表响应 =====");
        System.out.println("响应结果: 评论数=" + (result.getRecords() != null ? result.getRecords().size() : 0) + 
                           ", nextCursor=" + result.getNextCursor() + 
                           ", hasMore=" + result.getHasMore());
        
        return R.success(result);
    }
    
    @NoAuth
    @PostMapping("loadMoreChildren")
    @Operation(summary = "加载更多子评论", description = "根据父评论ID加载更多子评论")
    public R<ChildCommentCursorDto> loadMoreChildComments(@RequestBody JobLoadMoreChildrenDto request) {
        System.out.println("===== 加载更多子评论请求 =====");
        System.out.println("请求参数: jobId=" + request.getJobId() + 
                          ", parentId=" + request.getParentId() + 
                          ", cursor=" + request.getCursor());
        
        // 调用新的服务方法加载更多子评论
        ChildCommentCursorDto result = commentService.loadMoreChildComments(request);
        
        System.out.println("===== 加载更多子评论响应 =====");
        System.out.println("响应结果: 子评论数=" + 
                          (result.getRecords() != null ? result.getRecords().size() : 0) + 
                          ", nextCursor=" + result.getNextCursor() + 
                          ", hasMore=" + result.getHasMore() +
                          ", 总数=" + result.getTotal());
        
        return R.success(result);
    }
    
    @NoAuth
    @GetMapping("count")
    @Operation(summary = "获取评论数量", description = "获取职位评论数量")
    public R<Integer> getCommentsCount(@RequestParam("job_id") Integer jobId) {
        return R.success(commentService.getCommentsCount(jobId));
    }
    
    @NoAuth
    @GetMapping("childrenCount")
    @Operation(summary = "获取子评论数量", description = "获取某个顶级评论的子评论数量")
    public R<Integer> getChildrenCount(@RequestParam("job_id") Integer jobId, 
                                        @RequestParam("parent_id") Integer parentId) {
        System.out.println("===== 获取子评论数量请求 =====");
        System.out.println("请求参数: jobId=" + jobId + ", parentId=" + parentId);
        
        int childrenCount = commentService.getChildCommentsCount(jobId, parentId);
        
        System.out.println("===== 获取子评论数量响应 =====");
        System.out.println("响应结果: 子评论数量=" + childrenCount);
        
        return R.success(childrenCount);
    }
} 