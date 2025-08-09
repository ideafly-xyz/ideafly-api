package com.ideafly.controller.interact;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.common.RequestUtils;
import com.ideafly.dto.interact.ChildCommentCursorDto;
import com.ideafly.dto.interact.JobCommentInputDto;
import com.ideafly.dto.interact.JobCommentPageDto;
import com.ideafly.dto.interact.ParentCommentCursorDto;
import com.ideafly.dto.job.*;
import com.ideafly.service.impl.interact.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "评论相关接口", description = "评论相关接口")
@RestController
@RequestMapping("/api/comments")
@Slf4j
public class PostCommentController {

    @Resource
    private CommentService commentService;

    @PostMapping("add")
    @Operation(summary = "添加评论", description = "添加评论到职位")
    public R<Map<String, Object>> comment(@Valid @RequestBody JobCommentInputDto request, HttpServletRequest httpRequest) {
        String userId = RequestUtils.getCurrentUserId(httpRequest);
        if (userId == null) {
            return R.error("用户未登录");
        }
        Integer commentId = commentService.addComment(request, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", commentId);
        return R.success(result);
    }
    
    @NoAuth
    @PostMapping("list")
    @Operation(summary = "获取父评论列表(游标分页)", description = "使用游标分页获取父评论列表")
    public R<ParentCommentCursorDto> getParentCommentsByCursor(@RequestBody JobCommentPageDto request) {
        log.info("===== 父评论列表请求 =====");
        log.info("请求参数: {}", request);

        ParentCommentCursorDto result = commentService.getParentCommentsByCursor(request);

        int count = result.getRecords() != null ? result.getRecords().size() : 0;
        log.info("===== 父评论列表响应 =====");
        log.info("响应结果: 评论数={}, nextCursor={}, hasMore={}", count, result.getNextCursor(), result.getHasMore());

        return R.success(result);
    }
    
    @NoAuth
    @PostMapping("loadMoreChildren")
    @Operation(summary = "加载更多子评论", description = "根据父评论ID加载更多子评论")
    public R<ChildCommentCursorDto> loadMoreChildComments(@RequestBody JobLoadMoreChildrenDto request) {
        log.info("===== 加载更多子评论请求 =====");
        log.info("请求参数: jobId={}, parentId={}, cursor={}", request.getJobId(), request.getParentId(), request.getCursor());

        ChildCommentCursorDto result = commentService.loadMoreChildComments(request);

        int size = result.getRecords() != null ? result.getRecords().size() : 0;
        log.info("===== 加载更多子评论响应 =====");
        log.info("响应结果: 子评论数={}, nextCursor={}, hasMore={}, 总数={}", size, result.getNextCursor(), result.getHasMore(), result.getTotal());

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
        log.info("===== 获取子评论数量请求 =====");
        log.info("请求参数: jobId={}, parentId={}", jobId, parentId);

        int childrenCount = commentService.getChildCommentsCount(jobId, parentId);

        log.info("===== 获取子评论数量响应 =====");
        log.info("响应结果: 子评论数量={}", childrenCount);

        return R.success(childrenCount);
    }
} 