package com.ideafly.controller.h5;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.dto.job.*;
import com.ideafly.model.JobComments;
import com.ideafly.model.Jobs;
import com.ideafly.service.JobCommentsService;
import com.ideafly.service.JobFavoriteService;
import com.ideafly.service.JobLikesService;
import com.ideafly.service.JobsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Tag(name = "职位相关接口")
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
     * 发布职位接口
     */
    @PostMapping("createJob")
    public R<Boolean> createJob(@Valid @RequestBody CreateJobInputDto request) { //  使用 @Valid 注解开启参数校验
        Jobs job = jobService.createJob(request);
        return R.success(Boolean.TRUE);
    }
    @PostMapping("like")
    public R<Boolean> like(@Valid @RequestBody JobLikeInputDto request) { //  使用 @Valid 注解开启参数校验
      jobLikesService.addOrRemoveLike(request);
        return R.success(Boolean.TRUE);
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
}