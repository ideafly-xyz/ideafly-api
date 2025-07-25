package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.ideafly.common.*;
import com.ideafly.common.job.*;
import com.ideafly.dto.job.CreateJobInputDto;
import com.ideafly.dto.job.CursorResponseDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.dto.job.NextJobInputDto;
import com.ideafly.mapper.JobsMapper;
import com.ideafly.model.Jobs;
import com.ideafly.model.Users;
import com.ideafly.utils.CursorUtils;
import com.ideafly.utils.PageUtil;
import com.ideafly.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.lang.reflect.Field;

@Service
public class JobsService extends ServiceImpl<JobsMapper, Jobs> {
    @Resource
    private UsersService usersService;

    @Resource
    private JobFavoriteService jobFavoriteService;

    @Resource
    private JobLikesService jobLikesService;
    
    @Resource
    private CommentService commentService;
    
    @Resource
    private UserFollowService userFollowService;

    /**
     * 使用游标分页获取职位列表
     */
    public CursorResponseDto<JobDetailOutputDto> getJobsWithCursor(JobListInputDto request) {
        System.out.println("【性能日志】使用游标分页获取职位列表");
        long startTime = System.currentTimeMillis();
        
        // 默认每页大小，如果未指定
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(3); // 保持与前端一致的默认值
        }
        
        // 构建查询条件
        LambdaQueryWrapper<Jobs> queryWrapper = new LambdaQueryWrapper<>();
        
        // 解析游标
        String maxCursor = request.getMaxCursor();
        String minCursor = request.getMinCursor();
        
        boolean isForward = StringUtils.isNotBlank(maxCursor); // 向前查询（历史内容）
        boolean isBackward = StringUtils.isNotBlank(minCursor); // 向后查询（新内容）
        
        if (isForward && isBackward) {
            // 不能同时指定两个方向，以maxCursor为优先
            isBackward = false;
        }
        
        // 根据游标构建查询条件
        if (isForward) {
            // 解析maxCursor (向左滑，获取历史内容)
            Map<String, Object> maxCursorValues = CursorUtils.decodeCursor(maxCursor);
            if (maxCursorValues != null) {
                final Date forwardTimestamp = (Date) maxCursorValues.get("timestamp");
                final Integer forwardId = (Integer) maxCursorValues.get("id");
                
                // 构建查询条件：获取比当前游标更早的内容
                if (forwardTimestamp != null && forwardId != null) {
                    // 时间相同时按ID降序，时间比游标早或时间相同但ID更小
                    queryWrapper.and(w -> w
                            .lt(Jobs::getCreatedAt, forwardTimestamp)
                            .or(o -> o
                                    .eq(Jobs::getCreatedAt, forwardTimestamp)
                                    .lt(Jobs::getId, forwardId)
                            )
                    );
                }
            }
            // 按时间降序，同一时间按ID降序
            queryWrapper.orderByDesc(Jobs::getCreatedAt, Jobs::getId);
            
        } else if (isBackward) {
            // 解析minCursor (向右滑，获取更新内容)
            Map<String, Object> minCursorValues = CursorUtils.decodeCursor(minCursor);
            if (minCursorValues != null) {
                final Date backwardTimestamp = (Date) minCursorValues.get("timestamp");
                final Integer backwardId = (Integer) minCursorValues.get("id");
                
                // 构建查询条件：获取比当前游标更新的内容
                if (backwardTimestamp != null && backwardId != null) {
                    // 时间比游标新或时间相同但ID更大
                    queryWrapper.and(w -> w
                            .gt(Jobs::getCreatedAt, backwardTimestamp)
                            .or(o -> o
                                    .eq(Jobs::getCreatedAt, backwardTimestamp)
                                    .gt(Jobs::getId, backwardId)
                            )
                    );
                }
            }
            // 按时间升序，同一时间按ID升序（获取后需要反转）
            queryWrapper.orderByAsc(Jobs::getCreatedAt, Jobs::getId);
            
        } else {
            // 没有指定游标，获取最新内容
            queryWrapper.orderByDesc(Jobs::getCreatedAt, Jobs::getId);
        }
        
        // 查询数据
        Integer limit = request.getPageSize() + 1; // 多查一条用于判断是否有更多数据
        List<Jobs> jobs = this.list(queryWrapper.last("LIMIT " + limit));
        
        // 如果是向后查询，需要反转结果顺序
        if (isBackward && !jobs.isEmpty()) {
            Collections.reverse(jobs);
        }
        
        // 判断是否有更多数据
        boolean hasMore = jobs.size() > request.getPageSize();
        if (hasMore) {
            // 移除多查的一条数据
            jobs.remove(jobs.size() - 1);
        }
        
        // 处理空结果
        if (jobs.isEmpty()) {
            return new CursorResponseDto<>(
                    new ArrayList<>(),
                    maxCursor, // 保持原游标
                    minCursor, // 保持原游标
                    isForward ? hasMore : false,
                    isBackward ? hasMore : false,
                    0L
            );
        }
        
        // 计算下一个游标
        String nextMaxCursor = null;
        String nextMinCursor = null;
        
        if (!jobs.isEmpty()) {
            // 获取历史方向的下一个游标（最后一条记录）
            Jobs lastJob = jobs.get(jobs.size() - 1);
            nextMaxCursor = CursorUtils.encodeCursor(lastJob.getCreatedAt(), lastJob.getId());
            
            // 确保下一个maxCursor与请求的maxCursor不同
            if (nextMaxCursor.equals(maxCursor)) {
                System.out.println("【警告】新生成的maxCursor与请求的相同，可能导致重复数据");
                // 如果查询条件正确但结果游标相同，说明已经没有更多数据了
                // 覆盖hasMore标志，防止前端无限请求相同数据
                hasMore = false;
                
                // 输出最老记录信息，便于调试
                System.out.println("【特殊处理】强制设置hasMore=false，最后一条记录ID: " + lastJob.getId() + 
                                   ", 创建时间: " + lastJob.getCreatedAt());
            }
            
            // 获取新内容方向的下一个游标（第一条记录）
            Jobs firstJob = jobs.get(0);
            nextMinCursor = CursorUtils.encodeCursor(firstJob.getCreatedAt(), firstJob.getId());
        }
        
        // 处理作品数据，与现有逻辑保持一致
        // 转换为DTO
        List<JobDetailOutputDto> dtoList = processJobsForOutput(jobs);
        
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】游标分页获取职位列表完成 - 耗时: " + (endTime - startTime) + "ms");
        
        return new CursorResponseDto<>(
                dtoList,
                (!isForward && !isBackward && !jobs.isEmpty()) || (isForward && hasMore) ? nextMaxCursor : maxCursor,
                (!isForward && !isBackward && !jobs.isEmpty()) || (isBackward && hasMore) ? nextMinCursor : minCursor,
                isForward ? hasMore : true, // 历史方向是否有更多数据
                isBackward ? hasMore : false, // 新内容方向是否有更多数据（初始加载时为false，因为已加载最新数据）
                (long) dtoList.size()
        );
    }
    
    /**
     * 处理作品数据，转换为DTO
     */
    private List<JobDetailOutputDto> processJobsForOutput(List<Jobs> jobs) {
        if (jobs.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量获取所有用户ID
        Set<String> userIds = jobs.stream()
                .map(Jobs::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        List<Integer> jobIds = jobs.stream()
                .map(Jobs::getId)
                .collect(Collectors.toList());
        
        // 批量查询用户信息
        final Map<String, Users> userMap;
        if (!userIds.isEmpty()) {
            List<Users> users = usersService.listByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(Users::getId, user -> user, (a, b) -> a));
        } else {
            userMap = new HashMap<>();
        }
        
        // 获取当前用户
        String currentUserId = UserContextHolder.getUid();
        
        // 批量查询收藏和点赞状态
        final Map<Integer, Boolean> favoriteMap = new HashMap<>();
        final Map<Integer, Boolean> likeMap = new HashMap<>();
        
        // 批量查询点赞、收藏和评论数量
        final Map<Integer, Integer> likesCountMap = new HashMap<>();
        final Map<Integer, Integer> favoritesCountMap = new HashMap<>();
        final Map<Integer, Integer> commentsCountMap = new HashMap<>();
        
        // 批量获取统计数据
        if (!jobIds.isEmpty()) {
            try {
                // 批量查询点赞数
                for (Integer jobId : jobIds) {
                    int likesCount = jobLikesService.getJobLikesCount(jobId);
                    likesCountMap.put(jobId, likesCount);
                }
                
                // 批量查询收藏数
                for (Integer jobId : jobIds) {
                    int favoritesCount = jobFavoriteService.getJobFavoritesCount(jobId);
                    favoritesCountMap.put(jobId, favoritesCount);
                }
                
                // 批量查询评论数
                for (Integer jobId : jobIds) {
                    int commentsCount = commentService.getJobCommentsCount(jobId);
                    commentsCountMap.put(jobId, commentsCount);
                }
            } catch (Exception e) {
                System.out.println("【性能日志】批量获取统计数据异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (currentUserId != null && !jobIds.isEmpty()) {
            try {
                // 批量查询收藏状态
                Map<Integer, Boolean> tempFavoriteMap = jobFavoriteService.batchGetFavoriteStatus(jobIds, currentUserId);
                favoriteMap.putAll(tempFavoriteMap);
                
                // 批量查询点赞状态
                Map<Integer, Boolean> tempLikeMap = jobLikesService.batchGetLikeStatus(jobIds, currentUserId);
                likeMap.putAll(tempLikeMap);
            } catch (Exception e) {
                System.out.println("【性能日志】批量获取状态异常: " + e.getMessage());
                // 设置默认值
                jobIds.forEach(jobId -> {
                    favoriteMap.put(jobId, false);
                    likeMap.put(jobId, false);
                });
            }
        } else {
            // 用户未登录，所有职位设置默认值
            jobIds.forEach(jobId -> {
                favoriteMap.put(jobId, false);
                likeMap.put(jobId, false);
            });
        }
        
        // 转换DTO
        return jobs.stream().map(job -> {
            JobDetailOutputDto dto = BeanUtil.copyProperties(job, JobDetailOutputDto.class);
            
            // 设置职位基本信息
            dto.setPostTitle(job.getPostTitle());
            dto.setPostContent(job.getPostContent());
            
            // 设置统计数据
            dto.setLikes(likesCountMap.getOrDefault(job.getId(), 0));
            dto.setFavorites(favoritesCountMap.getOrDefault(job.getId(), 0));
            dto.setComments(commentsCountMap.getOrDefault(job.getId(), 0));
            dto.setShares(0); // 暂时设置为0
            
            // 设置用户信息
            Users user = userMap.get(job.getUserId());
            if (user != null) {
                dto.setPublisherName(user.getUsername());
                dto.setPublisherAvatar(user.getAvatar());
            }
            
            // 设置发布时间
            dto.setPublishTime(TimeUtils.formatRelativeTime(job.getCreatedAt()) + "发布");
            
            // 设置收藏和点赞状态
            dto.setIsLike(likeMap.getOrDefault(job.getId(), false));
            dto.setIsFavorite(favoriteMap.getOrDefault(job.getId(), false));
            
            // 设置游标值（前端可能需要）
            dto.setCursor(CursorUtils.encodeCursor(job.getCreatedAt(), job.getId()));
            
            return dto;
        }).collect(Collectors.toList());
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
        
        // 设置统计数据 (从各自的Service动态获取)
        long statsQueryStart = System.currentTimeMillis();
        try {
            int likesCount = jobLikesService.getJobLikesCount(job.getId());
            int favoritesCount = jobFavoriteService.getJobFavoritesCount(job.getId());
            int commentsCount = commentService.getJobCommentsCount(job.getId());
            
            dto.setLikes(likesCount);
            dto.setFavorites(favoritesCount);
            dto.setComments(commentsCount);
            dto.setShares(0); // 暂时设置为0，如果有共享计数表，可以从那里获取
        } catch (Exception e) {
            System.out.println("获取统计数据失败: " + e.getMessage());
            e.printStackTrace();
            // 设置默认值
            dto.setLikes(0);
            dto.setFavorites(0);
            dto.setComments(0);
            dto.setShares(0);
        }
        long statsQueryEnd = System.currentTimeMillis();
        
        if (statsQueryEnd - statsQueryStart > 50) {
            System.out.println("【性能日志】统计数据查询耗时较长 - 职位ID: " + job.getId() + ", 耗时: " + (statsQueryEnd - statsQueryStart) + "ms");
        }
        
        // 设置是否收藏和点赞状态
        long statusQueryStart = System.currentTimeMillis();
        try {
            // 获取当前用户ID
            String uid = UserContextHolder.getUid();
            
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

    // 新增重载方法，支持批量转换多个职位，减少重复查询
    public JobDetailOutputDto convertDto(
            Jobs job,
            Map<String, Users> userMap,
            Map<Integer, Integer> likesCountMap,
            Map<Integer, Integer> favoritesCountMap, 
            Map<Integer, Integer> commentsCountMap,
            Map<Integer, Boolean> favoriteMap,
            Map<Integer, Boolean> likeMap) {
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
        // 设置统计数据
        dto.setLikes(likesCountMap.getOrDefault(job.getId(), 0));
        dto.setFavorites(favoritesCountMap.getOrDefault(job.getId(), 0));
        dto.setComments(commentsCountMap.getOrDefault(job.getId(), 0));
        dto.setShares(0); // 暂时设置为0，如果有共享计数表，可以从那里获取
        // 设置收藏和点赞状态
        dto.setIsLike(likeMap.getOrDefault(job.getId(), false));
        dto.setIsFavorite(favoriteMap.getOrDefault(job.getId(), false));
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
    
    // 添加服务的getter方法
    public UsersService getUsersService() {
        return usersService;
    }
    
    public JobFavoriteService getJobFavoriteService() {
        return jobFavoriteService;
    }
    
    public JobLikesService getJobLikesService() {
        return jobLikesService;
    }
    
    public CommentService getJobCommentsService() {
        return commentService;
    }

    /**
     * 获取关注用户发布的帖子
     * @param request 分页请求参数
     * @return 关注用户的帖子列表（分页）
     */
    public Page<JobDetailOutputDto> getFollowingUserJobs(JobListInputDto request) {
        System.out.println("【性能日志】开始获取关注用户帖子 - 页大小:" + request.getPageSize());
        long startTime = System.currentTimeMillis();
        
        // 获取当前用户ID
        String currentUserId = UserContextHolder.getUid();
        if (currentUserId == null) {
            System.out.println("【性能日志】用户未登录，无法获取关注用户帖子");
            throw new IllegalArgumentException("用户未登录");
        }
        
        // 创建分页对象（固定使用第1页）
        Page<Jobs> page = new Page<>(1, request.getPageSize());
        
        try {
            // 获取当前用户关注的用户ID列表
            List<String> followingUserIds = userFollowService.getFollowingUserIds(currentUserId);
            
            // 如果没有关注任何用户，返回空结果
            if (CollUtil.isEmpty(followingUserIds)) {
                System.out.println("【性能日志】用户未关注任何人，返回空结果");
                return new Page<>();
            }
            
            System.out.println("【性能日志】用户关注了 " + followingUserIds.size() + " 个用户");
            
            // 查询关注用户发布的帖子
            Page<Jobs> jobsPage = this.lambdaQuery()
                    .in(Jobs::getUserId, followingUserIds)
                    .orderByDesc(Jobs::getCreatedAt)
                    .page(page);
            
            List<Jobs> jobs = jobsPage.getRecords();
            System.out.println("【性能日志】查询到 " + jobs.size() + " 条关注用户的帖子");
            
            if (jobs.isEmpty()) {
                return new Page<>();
            }
            
            // 批量获取所有用户ID
            Set<String> userIds = jobs.stream()
                .map(Jobs::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            List<Integer> jobIds = jobs.stream()
                .map(Jobs::getId)
                .collect(Collectors.toList());
            
            // 批量查询用户信息
            final Map<String, Users> userMap;
            if (!userIds.isEmpty()) {
                List<Users> users = usersService.listByIds(userIds);
                userMap = users.stream()
                    .collect(Collectors.toMap(Users::getId, user -> user, (a, b) -> a));
            } else {
                userMap = new HashMap<>();
            }
            
            // 批量查询统计数据
            final Map<Integer, Integer> likesCountMap = new HashMap<>();
            final Map<Integer, Integer> favoritesCountMap = new HashMap<>();
            final Map<Integer, Integer> commentsCountMap = new HashMap<>();
            final Map<Integer, Boolean> favoriteMap = new HashMap<>();
            final Map<Integer, Boolean> likeMap = new HashMap<>();
            
            if (!jobIds.isEmpty()) {
                // 批量查询点赞数
                for (Integer jobId : jobIds) {
                    likesCountMap.put(jobId, jobLikesService.getJobLikesCount(jobId));
                }
                
                // 批量查询收藏数
                for (Integer jobId : jobIds) {
                    favoritesCountMap.put(jobId, jobFavoriteService.getJobFavoritesCount(jobId));
                }
                
                // 批量查询评论数
                for (Integer jobId : jobIds) {
                    commentsCountMap.put(jobId, commentService.getJobCommentsCount(jobId));
                }
                
                // 批量查询收藏状态
                Map<Integer, Boolean> tempFavoriteMap = jobFavoriteService.batchGetFavoriteStatus(jobIds, currentUserId);
                favoriteMap.putAll(tempFavoriteMap);
                
                // 批量查询点赞状态
                Map<Integer, Boolean> tempLikeMap = jobLikesService.batchGetLikeStatus(jobIds, currentUserId);
                likeMap.putAll(tempLikeMap);
            }
            
            // 转换为DTO
            List<JobDetailOutputDto> dtoList = jobs.stream()
                .map(job -> convertDto(
                    job, 
                    userMap, 
                    likesCountMap, 
                    favoritesCountMap, 
                    commentsCountMap, 
                    favoriteMap, 
                    likeMap
                ))
                .collect(Collectors.toList());
            
            // 构建分页结果
            Page<JobDetailOutputDto> result = new Page<>();
            result.setRecords(dtoList);
            result.setTotal(jobsPage.getTotal());
            result.setPages(jobsPage.getPages());
            result.setCurrent(1);
            result.setSize(request.getPageSize());
            
            long endTime = System.currentTimeMillis();
            System.out.println("【性能日志】获取关注用户帖子完成 - 耗时:" + (endTime - startTime) + "ms, 记录数:" + dtoList.size());
            
            return result;
        } catch (Exception e) {
            System.out.println("【性能日志】获取关注用户帖子异常: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("获取关注用户帖子失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户发布的职位列表
     */
    public Object getUserPosts(JobListInputDto request, String userId) {
        System.out.println("【性能日志】获取用户职位列表 - 用户ID: " + userId);
        long startTime = System.currentTimeMillis();
        
        // 记录原始游标参数，便于调试
        String originalMaxCursor = request.getMaxCursor();
        System.out.println("【getUserPosts】原始maxCursor: " + originalMaxCursor);
        
        // 调用游标分页方法
        CursorResponseDto<JobDetailOutputDto> result = getUserPostsWithCursor(request, userId);
        
        // 如果结果为空且之前有指定maxCursor，可能是格式问题，进行特殊处理
        if (result.getRecords().isEmpty() && originalMaxCursor != null) {
            System.out.println("【特殊处理】使用游标查询返回空结果，尝试重新查询最新内容");
            
            // 清除游标参数，获取最新内容
            request.setMaxCursor(null);
            request.setMinCursor(null);
            result = getUserPostsWithCursor(request, userId);
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】获取用户职位列表完成 - 总耗时: " + (endTime - startTime) + "ms");
        
        return result;
    }
    
    /**
     * 使用游标分页获取用户职位
     */
    private CursorResponseDto<JobDetailOutputDto> getUserPostsWithCursor(JobListInputDto request, String userId) {
        System.out.println("【性能日志】使用游标分页获取用户职位列表");
        long startTime = System.currentTimeMillis();
        
        // 默认每页大小，仅在客户端未指定时设置
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(4); // 默认每页4个
        }
        
        System.out.println("【getUserPostsWithCursor】请求的页面大小: " + request.getPageSize());
        
        // 构建查询条件
        LambdaQueryWrapper<Jobs> queryWrapper = new LambdaQueryWrapper<>();
        
        // 只查询当前用户的作品
        queryWrapper.eq(Jobs::getUserId, userId);
        
        // 解析游标
        String maxCursor = request.getMaxCursor();
        String minCursor = request.getMinCursor();
        
        boolean isForward = StringUtils.isNotBlank(maxCursor); // 向前查询（历史内容）
        boolean isBackward = StringUtils.isNotBlank(minCursor); // 向后查询（新内容）
        
        if (isForward && isBackward) {
            // 不能同时指定两个方向，以maxCursor为优先
            isBackward = false;
        }
        
        // 保存解析后的游标值，用于后续处理
        Map<String, Object> maxCursorValues = null;
        Map<String, Object> minCursorValues = null;
        
        // 根据游标构建查询条件
        if (isForward) {
            // 解析maxCursor (向下滑动，获取历史内容)
            maxCursorValues = CursorUtils.decodeCursor(maxCursor);
            
            if (maxCursorValues != null) {
                final Date forwardTimestamp = (Date) maxCursorValues.get("timestamp");
                final Integer forwardId = (Integer) maxCursorValues.get("id");
                
                // 构建查询条件：获取比当前游标更早的内容
                if (forwardTimestamp != null && forwardId != null) {
                    // 添加排除当前游标指向的记录自身的条件
                    queryWrapper.and(w -> w
                            .lt(Jobs::getCreatedAt, forwardTimestamp)
                            .or(o -> o
                                    .eq(Jobs::getCreatedAt, forwardTimestamp)
                                    .lt(Jobs::getId, forwardId)
                            )
                    );
                    
                    System.out.println("【查询条件】历史方向: 时间 < " + forwardTimestamp + " 或 (时间 = " + forwardTimestamp + " 且 ID < " + forwardId + ")");
                    
                    // 打印解析后的游标值用于调试
                    System.out.println("【游标解析结果】maxCursor: " + maxCursor + 
                                      " -> timestamp: " + forwardTimestamp + 
                                      ", id: " + forwardId);
                } else {
                    System.out.println("【警告】游标解析错误 - 无法从maxCursor提取timestamp或id: " + maxCursor);
                }
            } else {
                System.out.println("【警告】游标解析失败 - maxCursor无法解码: " + maxCursor);
            }
        } else {
            // 没有指定游标，获取最新内容
            System.out.println("【查询条件】首次加载，获取最新内容");
        }
        
        // 保持创建时间和ID的双重排序，确保游标工作正常
        queryWrapper.orderByDesc(Jobs::getCreatedAt, Jobs::getId);
        
        // 多查询一条用于判断是否还有更多数据
        int limit = request.getPageSize() + 1;
        System.out.println("【getUserPostsWithCursor】实际查询限制数量: " + limit);
        
        // 打印最终SQL，确保条件正确添加
        String finalSql = queryWrapper.getCustomSqlSegment() + " LIMIT " + limit;
        System.out.println("【最终SQL条件】" + finalSql);
        
        // 执行查询
        List<Jobs> jobs = this.list(queryWrapper.last("LIMIT " + limit));
        
        System.out.println("【getUserPostsWithCursor】查询到原始记录数: " + jobs.size());
        
        // 记录查询结果中的所有ID，用于调试
        if (!jobs.isEmpty()) {
            String resultIds = jobs.stream()
                .map(job -> job.getId().toString())
                .collect(Collectors.joining(", "));
            System.out.println("【查询结果】职位ID列表: " + resultIds);
        } else {
            System.out.println("【查询结果】没有匹配的记录");
        }
        
        // 判断是否有更多数据
        boolean hasMore = jobs.size() > request.getPageSize();
        if (hasMore) {
            // 移除多查的一条数据
            jobs.remove(jobs.size() - 1);
            System.out.println("【getUserPostsWithCursor】移除额外记录后的记录数: " + jobs.size());
        }
        
        // 处理空结果
        if (jobs.isEmpty()) {
            System.out.println("【警告】查询结果为空，返回空响应");
            return new CursorResponseDto<>(
                    new ArrayList<>(),
                    maxCursor, // 保持原游标
                    minCursor, // 保持原游标
                    false,     // 如果结果为空，则没有更多数据
                    false,
                    0L
            );
        }
        
        // 计算下一个游标
        String nextMaxCursor = null;
        String nextMinCursor = null;
        
        // 获取历史方向的下一个游标（最后一条记录）
        Jobs lastJob = jobs.get(jobs.size() - 1);
        nextMaxCursor = CursorUtils.encodeCursor(lastJob.getCreatedAt(), lastJob.getId());
        System.out.println("【游标生成】nextMaxCursor已生成 - 基于最后一条记录: ID=" + lastJob.getId() + 
                          ", 创建时间=" + lastJob.getCreatedAt() + ", 生成游标=" + nextMaxCursor);
        
        // 获取新内容方向的下一个游标（第一条记录）
        Jobs firstJob = jobs.get(0);
        nextMinCursor = CursorUtils.encodeCursor(firstJob.getCreatedAt(), firstJob.getId());
        System.out.println("【游标生成】nextMinCursor已生成 - 基于第一条记录: ID=" + firstJob.getId() + 
                          ", 创建时间=" + firstJob.getCreatedAt() + ", 生成游标=" + nextMinCursor);
        
        // 检查游标是否正常更新
        boolean sameAsPrevious = isForward && nextMaxCursor.equals(maxCursor);
        if (sameAsPrevious) {
            System.out.println("【严重警告】下一个maxCursor与当前相同，这可能导致无限重复数据！");
            System.out.println("  - 当前maxCursor: " + maxCursor);
            System.out.println("  - 计算的nextMaxCursor: " + nextMaxCursor);
            System.out.println("  - 最后记录ID: " + lastJob.getId() + ", 创建时间: " + lastJob.getCreatedAt());
            
            // 设置没有更多数据，避免前端继续请求
            hasMore = false;
        }
        
        // 转换为DTO
        List<JobDetailOutputDto> dtoList = processJobsForOutput(jobs);
        
        // 确保游标被添加到JobDetailOutputDto中
        for (int i = 0; i < dtoList.size(); i++) {
            JobDetailOutputDto dto = dtoList.get(i);
            Jobs job = jobs.get(i);
            // 将游标添加到DTO中，方便前端使用
            String itemCursor = CursorUtils.encodeCursor(job.getCreatedAt(), job.getId());
            dto.setCursor(itemCursor);
            System.out.println("【DTO游标设置】为记录 " + job.getId() + " 设置游标: " + itemCursor);
        }
        
        // 输出调试信息
        System.out.println("【返回结果】");
        System.out.println("  - 记录数量: " + dtoList.size());
        System.out.println("  - 下一个maxCursor: " + nextMaxCursor);
        System.out.println("  - 下一个minCursor: " + nextMinCursor);
        System.out.println("  - 是否与前一个游标相同: " + sameAsPrevious);
        System.out.println("  - 是否有更多数据: " + hasMore);
        
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】游标分页获取用户职位列表完成 - 耗时: " + (endTime - startTime) + "ms");
        
        // 构建返回结果
        CursorResponseDto<JobDetailOutputDto> response = new CursorResponseDto<>(
                dtoList,
                nextMaxCursor,
                nextMinCursor,
                hasMore, // 是否有更多历史数据
                false,   // 是否有更多新数据（目前只支持向下加载更多，所以固定为false）
                (long) dtoList.size()
        );
        
        // 打印最终响应游标
        System.out.println("【最终响应游标确认】nextMaxCursor=" + response.getNextMaxCursor() + 
                          ", nextMinCursor=" + response.getNextMinCursor());
        
        return response;
    }
    
    // 切换职位点赞状态
} 