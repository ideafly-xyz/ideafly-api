package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.CreateJobInputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.mapper.JobsMapper;
import com.ideafly.model.Jobs;
import org.springframework.stereotype.Service;

@Service
public class JobsService extends ServiceImpl<JobsMapper, Jobs> {
    public IPage<Jobs> getJobList(JobListInputDto request) {
        Page<Jobs> page = new Page<>(request.getPageNum(), request.getPageSize());
        return this.lambdaQuery()
                .eq(StrUtil.isNotBlank(request.getCity()), Jobs::getCity, request.getCity())
                .eq(StrUtil.isNotBlank(request.getProfession()), Jobs::getProfession, request.getProfession())
                .eq(StrUtil.isNotBlank(request.getRecruitmentType()), Jobs::getRecruitmentType, request.getRecruitmentType())
                .page(page);
    }

    public Jobs createJob(CreateJobInputDto request) {
        Jobs job = BeanUtil.copyProperties(request, Jobs.class);
        job.setUserId(UserContextHolder.getUid());
        this.save(job);
        return job;
    }
}
