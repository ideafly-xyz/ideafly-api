package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.ideafly.common.*;
import com.ideafly.dto.job.CreateJobInputDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.dto.job.NextJobInputDto;
import com.ideafly.mapper.JobsMapper;
import com.ideafly.model.Jobs;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    public JobDetailOutputDto getNextOneJob(NextJobInputDto request) {
        Jobs job = this.lambdaQuery()
                .gt(Jobs::getId, request.getNextJobId())
                .last(IdeaFlyConstant.LIMIT_1)
                .orderByAsc(Jobs::getId)
                .one();
        if (Objects.isNull(job)) {
            return null;
        }
        JobDetailOutputDto dto = BeanUtil.copyProperties(job, JobDetailOutputDto.class);
        dto.setCityName(City.fromCode(job.getCity()).getDescription());
        dto.setIndustryDomainName(IndustryDomain.fromCode(job.getIndustryDomain()).getDescription());
        dto.setWorkTypeName(WorkType.fromCode(job.getWorkType()).getDescription());
        dto.setProfessionName(Profession.fromCode(job.getProfession()).getDescription());
        dto.setProfessionName(Profession.fromCode(job.getProfession()).getDescription());
        return dto;

    }

    public Jobs createJob(CreateJobInputDto request) {
        Jobs job = BeanUtil.copyProperties(request, Jobs.class);
        job.setUserId(UserContextHolder.getUid());
        this.save(job);
        return job;
    }
}
