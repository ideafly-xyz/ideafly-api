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
import java.util.*;
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
        List<Jobs> jobs = pageResult.getRecords();
        long dbQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】数据库查询耗时: " + (dbQueryEnd - dbQueryStart) + "ms, 记录数: " + jobs.size());
        
        if (jobs.isEmpty()) {
            System.out.println("【性能日志】职位列表为空，直接返回");
            return PageUtil.build(page, new ArrayList<>());
        }
        
        // 2. 批量获取所有用户ID
        long batchPrepStart = System.currentTimeMillis();
        Set<Integer> userIds = jobs.stream()
            .map(Jobs::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        List<Integer> jobIds = jobs.stream()
            .map(Jobs::getId)
            .collect(Collectors.toList());
        
        // 3. 批量查询用户信息（一次查询替代N次）
        final Map<Integer, Users> userMap;
        if (!userIds.isEmpty()) {
            long userBatchQueryStart = System.currentTimeMillis();
            List<Users> users = usersService.listByIds(userIds);
            userMap = users.stream()
                .collect(Collectors.toMap(Users::getId, user -> user, (a, b) -> a));
            long userBatchQueryEnd = System.currentTimeMillis();
            System.out.println("【性能日志】批量用户查询耗时: " + (userBatchQueryEnd - userBatchQueryStart) + "ms, 用户数: " + users.size());
        } else {
            userMap = new HashMap<>();
        }
        
        // 4. 获取当前用户
        Integer currentUserId = UserContextHolder.getUid();
        long batchPrepEnd = System.currentTimeMillis();
        System.out.println("【性能日志】批量查询准备耗时: " + (batchPrepEnd - batchPrepStart) + "ms");
        
        // 5. 批量查询收藏和点赞状态
        final Map<Integer, Boolean> favoriteMap = new HashMap<>();
        final Map<Integer, Boolean> likeMap = new HashMap<>();
        
        if (currentUserId != null && !jobIds.isEmpty()) {
            long statusBatchQueryStart = System.currentTimeMillis();
            
            // 批量查询收藏状态
            try {
                Map<Integer, Boolean> tempFavoriteMap = jobFavoriteService.batchGetFavoriteStatus(jobIds, currentUserId);
                favoriteMap.putAll(tempFavoriteMap);
                System.out.println("【性能日志】批量获取收藏状态成功，数量: " + favoriteMap.size());
            } catch (Exception e) {
                System.out.println("【性能日志】批量获取收藏状态异常: " + e.getMessage());
                // 设置默认值
                jobIds.forEach(jobId -> favoriteMap.put(jobId, false));
            }
            
            // 批量查询点赞状态
            try {
                Map<Integer, Boolean> tempLikeMap = jobLikesService.batchGetLikeStatus(jobIds, currentUserId);
                likeMap.putAll(tempLikeMap);
                System.out.println("【性能日志】批量获取点赞状态成功，数量: " + likeMap.size());
            } catch (Exception e) {
                System.out.println("【性能日志】批量获取点赞状态异常: " + e.getMessage());
                // 设置默认值
                jobIds.forEach(jobId -> likeMap.put(jobId, false));
            }
            
            long statusBatchQueryEnd = System.currentTimeMillis();
            System.out.println("【性能日志】批量状态查询耗时: " + (statusBatchQueryEnd - statusBatchQueryStart) + "ms");
        } else {
            // 用户未登录，所有职位设置默认值
            System.out.println("【性能日志】用户未登录或职位列表为空，设置默认收藏和点赞状态");
            jobIds.forEach(jobId -> {
                favoriteMap.put(jobId, false);
                likeMap.put(jobId, false);
            });
        }
        
        // 6. 转换DTO（使用批量查询结果）
        long dtoConvertStart = System.currentTimeMillis();
        List<JobDetailOutputDto> dtoList = jobs.stream().map(job -> {
            JobDetailOutputDto dto = BeanUtil.copyProperties(job, JobDetailOutputDto.class);
            
            // 设置职位基本信息
            dto.setPostTitle(job.getPostTitle());
            dto.setPostContent(job.getPostContent());
            
            // 设置用户信息（从Map获取，避免查询）
            Users user = userMap.get(job.getUserId());
            if (user != null) {
                dto.setPublisherName(user.getUsername());
                dto.setPublisherAvatar(user.getAvatar());
            }
            
            // 设置发布时间
            dto.setPublishTime(TimeUtils.formatRelativeTime(job.getCreatedAt()) + "发布");
            
            // 设置收藏和点赞状态（从Map获取，避免查询）
            dto.setIsLike(likeMap.getOrDefault(job.getId(), false));
            dto.setIsFavorite(favoriteMap.getOrDefault(job.getId(), false));
            
            return dto;
        }).collect(Collectors.toList());
        long dtoConvertEnd = System.currentTimeMillis();
        System.out.println("【性能日志】DTO批量转换耗时: " + (dtoConvertEnd - dtoConvertStart) + "ms, 平均每条: " + 
                (dtoList.size() > 0 ? (dtoConvertEnd - dtoConvertStart) / dtoList.size() : 0) + "ms");
        
        // 7. 构建结果并返回
        Page<JobDetailOutputDto> result = PageUtil.build(page, dtoList);
        
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】职位列表获取完成 - 批量优化后总耗时: " + (endTime - startTime) + "ms");
        
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
