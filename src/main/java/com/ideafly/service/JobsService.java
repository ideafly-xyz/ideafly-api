package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.ideafly.common.*;
import com.ideafly.common.job.*;
import com.ideafly.dto.job.CreateJobInputDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.dto.job.NextJobInputDto;
import com.ideafly.mapper.JobsMapper;
import com.ideafly.model.Jobs;
import com.ideafly.model.Users;
import com.ideafly.utils.PageUtil;
import com.ideafly.utils.TimeUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class JobsService extends ServiceImpl<JobsMapper, Jobs> {
    @Resource
    private UsersService usersService;
    @Resource
    private JobCommentsService jobCommentsService;
    @Resource
    private JobLikesService jobLikesService;
    @Resource
    private JobFavoriteService jobFavoriteService;

    public Page<JobDetailOutputDto> getJobList(JobListInputDto request) {
        Page<Jobs> page = PageUtil.build(request);
        Page<Jobs> pageResult = this.lambdaQuery()
                .eq(StrUtil.isNotBlank(request.getCity()), Jobs::getCity, request.getCity())
                .eq(StrUtil.isNotBlank(request.getProfession()), Jobs::getProfession, request.getProfession())
                .eq(StrUtil.isNotBlank(request.getRecruitmentType()), Jobs::getRecruitmentType, request.getRecruitmentType())
                .orderByDesc(Jobs::getId)
                .page(page);
        List<JobDetailOutputDto> list = pageResult.getRecords().stream().map(this::convertDto).collect(Collectors.toList());
       return PageUtil.build(page,list);
    }

    public JobDetailOutputDto convertDto(Jobs job) {
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
        dto.setComments(jobCommentsService.getJobCommentCount(job.getId()));
        dto.setLikes(jobLikesService.getJobLikesCount(job.getId()));
        dto.setDislikes(4);
        dto.setIsFavorite(jobFavoriteService.isJobFavorite(job.getId()));
        dto.setIsLike(jobLikesService.isJobLike(job.getId()));
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
