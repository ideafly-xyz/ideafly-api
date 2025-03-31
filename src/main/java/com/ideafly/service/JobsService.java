package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
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
import com.ideafly.model.Users;
import com.ideafly.utils.TimeUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
public class JobsService extends ServiceImpl<JobsMapper, Jobs> {
    @Resource
    private UsersService usersService;

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
                .gt(request.getDirection() > 0, Jobs::getId, request.getCurrentJobId())
                .lt(request.getDirection() < 0, Jobs::getId, request.getCurrentJobId())
                .last(IdeaFlyConstant.LIMIT_1)
                .orderByAsc(Jobs::getId)
                .one();
        if (Objects.isNull(job)) {
            return null;
        }
        JobDetailOutputDto dto = BeanUtil.copyProperties(job, JobDetailOutputDto.class);
        Users user = usersService.getById(job.getUserId());
        if (Objects.nonNull(user)) {
            dto.setPublisherName(user.getNickname());
            dto.setPublisherAvatar(user.getAvatarUrl());
        }
        dto.setTags(CollUtil.newArrayList(City.fromCode(job.getCity()).getDescription(),
                IndustryDomain.fromCode(job.getIndustryDomain()).getDescription(),
                WorkType.fromCode(job.getWorkType()).getDescription(),
                Profession.fromCode(job.getProfession()).getDescription(),
                RecruitmentType.fromCode(job.getRecruitmentType()).getDescription()
        ));
        dto.setSkills(CollUtil.newArrayList("Java", "Spring", "MySQL"));
        dto.setSalary("10k-20k");
        dto.setPublishTime(TimeUtils.formatRelativeTime(job.getCreatedAt()) + "发布");
        dto.setComments(2);
        dto.setLikes(3);
        dto.setDislikes(4);
        dto.setIsFavorite(true);
        dto.setIsLike(true);
        dto.setIsDislike(false);
        return dto;

    }

    public Jobs createJob(CreateJobInputDto request) {
        Jobs job = BeanUtil.copyProperties(request, Jobs.class);
        job.setUserId(UserContextHolder.getUid());
        this.save(job);
        return job;
    }
}
