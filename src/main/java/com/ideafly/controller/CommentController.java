package com.ideafly.controller;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.dto.job.JobCommentCursorDto;
import com.ideafly.dto.job.JobCommentInputDto;
import com.ideafly.dto.job.JobCommentPageDto;
import com.ideafly.service.JobCommentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

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
        return R.success(jobCommentsService.getCommentsByCursor(request));
    }

    @NoAuth
    @GetMapping("count")
    @Operation(summary = "获取评论数量", description = "获取指定职位的评论数量")
    public R<Integer> getCommentsCount(@RequestParam(name = "job_id") Integer jobId) {
        return R.success(jobCommentsService.getJobCommentsCount(jobId));
    }
} 