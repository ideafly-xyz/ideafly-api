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
        long startTime = System.currentTimeMillis();
        System.out.println("【性能日志】开始获取职位列表 - 参数: " + request);
        
        // 1. 构建分页对象并查询数据库
        long dbQueryStart = System.currentTimeMillis();
        Page<Jobs> page = PageUtil.build(request);
        Page<Jobs> pageResult = this.lambdaQuery()
                .orderByDesc(Jobs::getId)
                .page(page);
        long dbQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】数据库查询耗时: " + (dbQueryEnd - dbQueryStart) + "ms, 记录数: " + pageResult.getRecords().size());
        
        // 2. 转换DTO (这是最耗时的部分，因为要查询用户、点赞、收藏等信息)
        long dtoConvertStart = System.currentTimeMillis();
        List<JobDetailOutputDto> list = pageResult.getRecords().stream().map(job -> {
            long singleDtoStart = System.currentTimeMillis();
            JobDetailOutputDto dto = this.convertDto(job);
            long singleDtoEnd = System.currentTimeMillis();
            
            // 记录每个DTO转换的耗时
            if (singleDtoEnd - singleDtoStart > 100) { // 只记录耗时较长的转换
                System.out.println("【性能日志】单个职位转换耗时过长 - 职位ID: " + job.getId() + ", 耗时: " + (singleDtoEnd - singleDtoStart) + "ms");
            }
            
            return dto;
        }).collect(Collectors.toList());
        long dtoConvertEnd = System.currentTimeMillis();
        System.out.println("【性能日志】DTO转换总耗时: " + (dtoConvertEnd - dtoConvertStart) + "ms, 平均每条: " + 
                (list.size() > 0 ? (dtoConvertEnd - dtoConvertStart) / list.size() : 0) + "ms");
        
        // 3. 构建结果并返回
        Page<JobDetailOutputDto> result = PageUtil.build(page, list);
        
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】职位列表获取完成 - 总耗时: " + (endTime - startTime) + "ms");
        
        return result;
    }

    public JobDetailOutputDto convertDto(Jobs job) {
        long startTime = System.currentTimeMillis();
        JobDetailOutputDto dto = BeanUtil.copyProperties(job, JobDetailOutputDto.class);
        
        // 设置title和content字段
        dto.setPostTitle(job.getPostTitle());
        dto.setPostContent(job.getPostContent());
        
        // 获取用户信息 (可能耗时)
        long userQueryStart = System.currentTimeMillis();
        Users user = usersService.getById(job.getUserId());
        long userQueryEnd = System.currentTimeMillis();
        
        if (userQueryEnd - userQueryStart > 50) {
            System.out.println("【性能日志】用户信息查询耗时较长 - 职位ID: " + job.getId() + ", 用户ID: " + job.getUserId() + ", 耗时: " + (userQueryEnd - userQueryStart) + "ms");
        }
        
        if (Objects.nonNull(user)) {
            dto.setPublisherName(user.getUsername());
            dto.setPublisherAvatar(user.getAvatar());
        }

        dto.setPublishTime(TimeUtils.formatRelativeTime(job.getCreatedAt()) + "发布");
        
        // 设置是否收藏和点赞状态 (这部分最可能耗时)
        long statusQueryStart = System.currentTimeMillis();
        try {
            // 获取当前用户ID
            Integer uid = UserContextHolder.getUid();
            
            // 默认设置为false
            dto.setIsLike(false);
            dto.setIsFavorite(false);
            
            // 只有登录用户才能获取收藏和点赞状态
            if (uid != null) {
                // 查询是否已收藏
                long favoriteQueryStart = System.currentTimeMillis();
                try {
                    Boolean isFavorite = jobFavoriteService.isJobFavorite(job.getId());
                    dto.setIsFavorite(isFavorite);
                    long favoriteQueryEnd = System.currentTimeMillis();
                    if (favoriteQueryEnd - favoriteQueryStart > 50) {
                        System.out.println("【性能日志】收藏状态查询耗时较长 - 职位ID: " + job.getId() + ", 耗时: " + (favoriteQueryEnd - favoriteQueryStart) + "ms");
                    }
                } catch (Exception e) {
                    System.out.println("获取收藏状态异常: " + e.getMessage());
                }
                
                // 查询是否已点赞
                long likeQueryStart = System.currentTimeMillis();
                try {
                    Boolean isLike = jobLikesService.isJobLikedByUser(job.getId(), uid);
                    dto.setIsLike(isLike);
                    long likeQueryEnd = System.currentTimeMillis();
                    if (likeQueryEnd - likeQueryStart > 50) {
                        System.out.println("【性能日志】点赞状态查询耗时较长 - 职位ID: " + job.getId() + ", 耗时: " + (likeQueryEnd - likeQueryStart) + "ms");
                    }
                } catch (Exception e) {
                    System.out.println("获取点赞状态异常: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("获取作品收藏/点赞状态失败: " + e.getMessage());
            e.printStackTrace();
        }
        long statusQueryEnd = System.currentTimeMillis();
        
        if (statusQueryEnd - statusQueryStart > 100) {
            System.out.println("【性能日志】状态查询总耗时较长 - 职位ID: " + job.getId() + ", 总耗时: " + (statusQueryEnd - statusQueryStart) + "ms");
        }
        
        long endTime = System.currentTimeMillis();
        if (endTime - startTime > 150) {
            System.out.println("【性能日志】单个职位DTO转换总耗时过长 - 职位ID: " + job.getId() + ", 总耗时: " + (endTime - startTime) + "ms");
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
