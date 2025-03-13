package com.ideafly.controller.h5;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ideafly.common.R;
import com.ideafly.dto.job.CreateJobInputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.model.Jobs;
import com.ideafly.service.JobsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

@Tag(name = "职位相关接口")
@RestController
@RequestMapping("/api/jobs")
public class JobH5Controller {

    @Resource
    private JobsService jobService;


    @PostMapping("list")
    public R<IPage<Jobs>> getJobList(@RequestBody JobListInputDto request) {
        IPage<Jobs> page = jobService.getJobList(request);
        return R.success(page);
    }

    /**
     * 发布职位接口
     */
    @PostMapping("createJob")
    public R<Boolean> createJob(@Valid @RequestBody CreateJobInputDto request) { //  使用 @Valid 注解开启参数校验
        Jobs job = jobService.createJob(request);
        return R.success(Boolean.TRUE);
    }
}