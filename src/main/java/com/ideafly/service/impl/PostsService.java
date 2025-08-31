package com.ideafly.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.dto.job.CreateJobInputDto;
import com.ideafly.dto.job.CursorResponseDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.mapper.JobsMapper;
import com.ideafly.model.Jobs;
import com.ideafly.model.users.Users;
import com.ideafly.service.UserFollowService;
import com.ideafly.service.impl.interact.CommentService;
import com.ideafly.service.impl.interact.JobFavoriteService;
import com.ideafly.service.impl.interact.JobLikesService;
import com.ideafly.service.impl.users.UsersService;
import com.ideafly.utils.CursorUtils;
import com.ideafly.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PostsService extends ServiceImpl<JobsMapper, Jobs> {
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
                log.warn("新生成的maxCursor与请求的相同，可能导致重复数据");
                // 如果查询条件正确但结果游标相同，说明已经没有更多数据了
                // 覆盖hasMore标志，防止前端无限请求相同数据
                hasMore = false;
                
                // 输出最老记录信息，便于调试
                log.warn("强制设置hasMore=false，最后一条记录ID: {}, 创建时间: {}", lastJob.getId(), lastJob.getCreatedAt());
            }
            
            // 获取新内容方向的下一个游标（第一条记录）
            Jobs firstJob = jobs.get(0);
            nextMinCursor = CursorUtils.encodeCursor(firstJob.getCreatedAt(), firstJob.getId());
        }
        
        // 处理作品数据，与现有逻辑保持一致
        // 转换为DTO
        List<JobDetailOutputDto> dtoList = processJobsForOutput(jobs);
        
        long endTime = System.currentTimeMillis();
        log.info("游标分页获取职位列表完成 - 耗时: {}ms", (endTime - startTime));
        
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
        
        // 批量查询收藏和点赞状态
        
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
                log.error("批量获取统计数据异常: {}", e.getMessage(), e);
            }
        }
        
        // 转换为DTO
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
            
            // 设置收藏和点赞状态（未登录用户默认为false）
            dto.setIsLike(false);
            dto.setIsFavorite(false);
            
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
            log.warn("用户信息查询耗时较长 - 职位ID: {}, 用户ID: {}, 耗时: {}ms", job.getId(), job.getUserId(), (userQueryEnd - userQueryStart));
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
            log.error("获取统计数据失败: {}", e.getMessage(), e);
            // 设置默认值
            dto.setLikes(0);
            dto.setFavorites(0);
            dto.setComments(0);
            dto.setShares(0);
        }
        long statsQueryEnd = System.currentTimeMillis();
        
        if (statsQueryEnd - statsQueryStart > 50) {
            log.warn("统计数据查询耗时较长 - 职位ID: {}, 耗时: {}ms", job.getId(), (statsQueryEnd - statsQueryStart));
        }
        
        // 设置是否收藏和点赞状态（未登录用户默认为false）
        dto.setIsLike(false);
        dto.setIsFavorite(false);
        
        long endTime = System.currentTimeMillis();
        if (endTime - startTime > 150) {
            log.warn("单个职位DTO转换总耗时过长 - 职位ID: {}, 总耗时: {}ms", job.getId(), (endTime - startTime));
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

    public Jobs createJob(CreateJobInputDto request, String userId) {
        Jobs job = new Jobs();
        // 只设置标题和内容字段
        job.setPostTitle(request.getPostTitle());
        job.setPostContent(request.getPostContent());
        job.setUserId(userId);
        this.save(job);
        return job;
    }
    
    /**
     * 删除帖子
     * @param jobId 帖子ID
     * @param userId 操作用户ID
     * @return 删除是否成功
     */
    public boolean deleteJob(Integer jobId, String userId) {
        log.info("尝试删除帖子 - 帖子ID: {}, 操作用户ID: {}", jobId, userId);
        if (jobId == null || userId == null) {
            log.warn("删除帖子失败：jobId或userId为空");
            return false;
        }
        
        // 查找帖子，确保是该用户发布的
        Jobs job = this.lambdaQuery()
                        .eq(Jobs::getId, jobId)
                        .eq(Jobs::getUserId, userId)
                        .one();
        
        if (job == null) {
            log.warn("删除帖子失败：未找到指定帖子或用户无权限 - 帖子ID: {}, 操作用户ID: {}", jobId, userId);
            return false; // 帖子不存在或用户无权删除
        }
        
        // 执行删除
        boolean deleted = this.removeById(jobId);
        if (deleted) {
            log.info("帖子删除成功 - 帖子ID: {}", jobId);
            // TODO: 这里可以考虑删除关联的点赞、收藏、评论等数据
        } else {
            log.error("帖子删除失败（数据库操作失败） - 帖子ID: {}", jobId);
        }
        return deleted;
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
     * @param userId 当前用户ID
     * @return 关注用户的帖子列表（分页）
     */
    public Page<JobDetailOutputDto> getFollowingUserJobs(JobListInputDto request, String userId) {
        log.info("开始获取关注用户帖子 - 页大小:{}", request.getPageSize());
        long startTime = System.currentTimeMillis();
        
        if (userId == null) {
            log.warn("用户未登录，无法获取关注用户帖子");
            throw new IllegalArgumentException("用户未登录");
        }
        
        // 创建分页对象（固定使用第1页）
        Page<Jobs> page = new Page<>(1, request.getPageSize());
        
        try {
            // 获取当前用户关注的用户ID列表
            List<String> followingUserIds = userFollowService.getFollowingUserIds(userId);
            
            // 如果没有关注任何用户，返回空结果
            if (CollUtil.isEmpty(followingUserIds)) {
                log.info("用户未关注任何人，返回空结果");
                return new Page<>();
            }
            
            log.info("用户关注了 {} 个用户", followingUserIds.size());
            
            // 查询关注用户发布的帖子
            Page<Jobs> jobsPage = this.lambdaQuery()
                    .in(Jobs::getUserId, followingUserIds)
                    .orderByDesc(Jobs::getCreatedAt)
                    .page(page);
            
            List<Jobs> jobs = jobsPage.getRecords();
            log.info("查询到 {} 条关注用户的帖子", jobs.size());
            
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
                Map<Integer, Boolean> tempFavoriteMap = jobFavoriteService.batchGetFavoriteStatus(jobIds, userId);
                favoriteMap.putAll(tempFavoriteMap);
                
                // 批量查询点赞状态
                Map<Integer, Boolean> tempLikeMap = jobLikesService.batchGetLikeStatus(jobIds, userId);
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
            log.info("获取关注用户帖子完成 - 耗时:{}ms, 记录数:{}", (endTime - startTime), dtoList.size());
            
            return result;
        } catch (Exception e) {
            log.error("获取关注用户帖子异常: {}", e.getMessage(), e);
            throw new IllegalArgumentException("获取关注用户帖子失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户发布的职位列表
     */
    public Object getUserPosts(JobListInputDto request, String userId) {
        log.info("获取用户职位列表 - 用户ID: {}", userId);
        long startTime = System.currentTimeMillis();
        
        // 记录原始游标参数，便于调试
        String originalMaxCursor = request.getMaxCursor();
        log.debug("getUserPosts 原始maxCursor: {}", originalMaxCursor);
        
        // 调用游标分页方法
        CursorResponseDto<JobDetailOutputDto> result = getUserPostsWithCursor(request, userId);
        
        // 如果结果为空且之前有指定maxCursor，可能是格式问题，进行特殊处理
        if (result.getRecords().isEmpty() && originalMaxCursor != null) {
            log.warn("使用游标查询返回空结果，尝试重新查询最新内容");
            
            // 清除游标参数，获取最新内容
            request.setMaxCursor(null);
            request.setMinCursor(null);
            result = getUserPostsWithCursor(request, userId);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("获取用户职位列表完成 - 总耗时: {}ms", (endTime - startTime));
        
        return result;
    }
    
    /**
     * 使用游标分页获取用户职位
     */
    private CursorResponseDto<JobDetailOutputDto> getUserPostsWithCursor(JobListInputDto request, String userId) {
        log.info("使用游标分页获取用户职位列表");
        long startTime = System.currentTimeMillis();
        
        // 默认每页大小，仅在客户端未指定时设置
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(4); // 默认每页4个
        }
        
        log.debug("getUserPostsWithCursor 请求的页面大小: {}", request.getPageSize());
        
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
                    
                    log.debug("查询条件：历史方向: 时间 < {}, 或 (时间 = {}, 且 ID < {})", forwardTimestamp, forwardTimestamp, forwardId);
                    
                    // 打印解析后的游标值用于调试
                    log.debug("游标解析结果 maxCursor: {}, -> timestamp: {}, id: {}", maxCursor, forwardTimestamp, forwardId);
                } else {
                    log.warn("游标解析错误 - 无法从maxCursor提取timestamp或id: {}", maxCursor);
                }
            } else {
                log.warn("游标解析失败 - maxCursor无法解码: {}", maxCursor);
            }
        } else {
            // 没有指定游标，获取最新内容
            log.debug("查询条件：首次加载，获取最新内容");
        }
        
        // 保持创建时间和ID的双重排序，确保游标工作正常
        queryWrapper.orderByDesc(Jobs::getCreatedAt, Jobs::getId);
        
        // 多查询一条用于判断是否还有更多数据
        int limit = request.getPageSize() + 1;
        log.debug("getUserPostsWithCursor 实际查询限制数量: {}", limit);
        
        // 打印最终SQL，确保条件正确添加
        String finalSql = queryWrapper.getCustomSqlSegment() + " LIMIT " + limit;
        log.debug("最终SQL条件: {}", finalSql);
        
        // 执行查询
        List<Jobs> jobs = this.list(queryWrapper.last("LIMIT " + limit));
        
        log.debug("getUserPostsWithCursor 查询到原始记录数: {}", jobs.size());
        
        // 记录查询结果中的所有ID，用于调试
        if (!jobs.isEmpty()) {
            String resultIds = jobs.stream()
                .map(job -> job.getId().toString())
                .collect(Collectors.joining(", "));
            log.debug("查询结果 职位ID列表: {}", resultIds);
        } else {
            log.debug("查询结果 没有匹配的记录");
        }
        
        // 判断是否有更多数据
        boolean hasMore = jobs.size() > request.getPageSize();
        if (hasMore) {
            // 移除多查的一条数据
            jobs.remove(jobs.size() - 1);
            log.debug("getUserPostsWithCursor 移除额外记录后的记录数: {}", jobs.size());
        }
        
        // 处理空结果
        if (jobs.isEmpty()) {
            log.warn("查询结果为空，返回空响应");
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
        log.debug("游标生成 nextMaxCursor已生成 - 基于最后一条记录: ID={}, 创建时间={}, 生成游标={}", lastJob.getId(), lastJob.getCreatedAt(), nextMaxCursor);
        
        // 获取新内容方向的下一个游标（第一条记录）
        Jobs firstJob = jobs.get(0);
        nextMinCursor = CursorUtils.encodeCursor(firstJob.getCreatedAt(), firstJob.getId());
        log.debug("游标生成 nextMinCursor已生成 - 基于第一条记录: ID={}, 创建时间={}, 生成游标={}", firstJob.getId(), firstJob.getCreatedAt(), nextMinCursor);
        
        // 检查游标是否正常更新
        boolean sameAsPrevious = isForward && nextMaxCursor.equals(maxCursor);
        if (sameAsPrevious) {
            log.warn("下一个maxCursor与当前相同，这可能导致无限重复数据！");
            log.warn("  - 当前maxCursor: {}", maxCursor);
            log.warn("  - 计算的nextMaxCursor: {}", nextMaxCursor);
            log.warn("  - 最后记录ID: {}, 创建时间: {}", lastJob.getId(), lastJob.getCreatedAt());
            
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
            log.debug("DTO游标设置 为记录 {} 设置游标: {}", job.getId(), itemCursor);
        }
        
        // 输出调试信息
        log.debug("返回结果");
        log.debug("  - 记录数量: {}", dtoList.size());
        log.debug("  - 下一个maxCursor: {}", nextMaxCursor);
        log.debug("  - 下一个minCursor: {}", nextMinCursor);
        log.debug("  - 是否与前一个游标相同: {}", sameAsPrevious);
        log.debug("  - 是否有更多数据: {}", hasMore);
        
        long endTime = System.currentTimeMillis();
        log.info("游标分页获取用户职位列表完成 - 耗时: {}ms", (endTime - startTime));
        
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
        log.debug("最终响应游标确认 nextMaxCursor={}, nextMinCursor={}", response.getNextMaxCursor(), response.getNextMinCursor());
        
        return response;
    }
    

} 