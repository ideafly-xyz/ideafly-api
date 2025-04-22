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
    private JobFavoriteService jobFavoriteService;

    @Resource
    private JobLikesService jobLikesService;

    public Page<JobDetailOutputDto> getJobList(JobListInputDto request) {
        Page<Jobs> page = PageUtil.build(request);
        Page<Jobs> pageResult = this.lambdaQuery()
                .orderByDesc(Jobs::getId)
                .page(page);
        List<JobDetailOutputDto> list = pageResult.getRecords().stream().map(this::convertDto).collect(Collectors.toList());
        return PageUtil.build(page, list);
    }

    public JobDetailOutputDto convertDto(Jobs job) {
        JobDetailOutputDto dto = BeanUtil.copyProperties(job, JobDetailOutputDto.class);
        // 设置title和content字段
        dto.setPostTitle(job.getPostTitle());
        dto.setPostContent(job.getPostContent());
        
        Users user = usersService.getById(job.getUserId());
        if (Objects.nonNull(user)) {
            dto.setPublisherName(user.getUsername());
            dto.setPublisherAvatar(user.getAvatar());
        }
        // 设置空标签和技能
        dto.setTags(CollUtil.newArrayList());
        dto.setSkills(CollUtil.newArrayList());
        dto.setPublishTime(TimeUtils.formatRelativeTime(job.getCreatedAt()) + "发布");
        
        // 设置是否收藏和点赞状态
        try {
            // 获取当前用户ID
            Integer uid = UserContextHolder.getUid();
            System.out.println("转换DTO - 职位ID:" + job.getId() + ", 当前用户ID:" + uid);
            
            // 默认设置为false
            dto.setIsLike(false);
            dto.setIsFavorite(false);
            
            // 只有登录用户才能获取收藏和点赞状态
            if (uid != null) {
                // 查询是否已收藏
                try {
                    Boolean isFavorite = jobFavoriteService.isJobFavorite(job.getId());
                    dto.setIsFavorite(isFavorite);
                    System.out.println("职位ID:" + job.getId() + ", 收藏状态:" + isFavorite);
                } catch (Exception e) {
                    System.out.println("获取收藏状态异常: " + e.getMessage());
                }
                
                // 查询是否已点赞
                try {
                    Boolean isLike = jobLikesService.isJobLikedByUser(job.getId(), uid);
                    dto.setIsLike(isLike);
                    System.out.println("职位ID:" + job.getId() + ", 点赞状态:" + isLike);
                } catch (Exception e) {
                    System.out.println("获取点赞状态异常: " + e.getMessage());
                }
            } else {
                System.out.println("用户未登录，设置默认收藏和点赞状态为false");
            }
        } catch (Exception e) {
            // 如果查询失败，打印错误日志，但仍使用默认值
            System.out.println("获取作品收藏/点赞状态失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return dto;
    }

    public Jobs createJob(CreateJobInputDto request) {
        Jobs job = new Jobs();
        // 只设置标题和内容字段
        job.setPostTitle(request.getPostTitle());
        job.setPostContent(request.getPostContent());
        job.setUserId(UserContextHolder.getUid());
        this.save(job);
        return job;
    }

    public void likes(Integer id, boolean isLike) {
        this.lambdaUpdate().setSql("likes = likes + " + (isLike ? 1 : -1)).eq(Jobs::getId, id).update();
    }
    public void comments(Integer id, boolean isComment) {
        this.lambdaUpdate().setSql("comments = comments + " + (isComment ? 1 : -1)).eq(Jobs::getId, id).update();
    }
    public void favorites(Integer id, boolean isFavorite) {
        this.lambdaUpdate().setSql("favorites = favorites + " + (isFavorite ? 1 : -1)).eq(Jobs::getId, id).update();
    }
}
