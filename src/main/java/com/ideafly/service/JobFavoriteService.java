package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.CursorResponseDto;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.model.JobFavorite;
import com.ideafly.model.Jobs;
import com.ideafly.model.Users;
import com.ideafly.utils.CursorUtils;
import com.ideafly.utils.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class JobFavoriteService extends ServiceImpl<JobFavoriteMapper, JobFavorite> {

    @Resource
    private JobsService jobsService;
    
    /**
     * 获取用户收藏的职位列表
     */
    public Object getUserFavoriteJobs(JobListInputDto request) {
        long startTime = System.currentTimeMillis();
        System.out.println("【性能日志】开始获取用户收藏职位列表 - 参数: " + request);
        
        // 检查是否使用游标分页
        if (Boolean.TRUE.equals(request.getUseCursor())) {
            return getUserFavoriteJobsWithCursor(request);
        } else {
            // 使用原有的传统分页逻辑
            return getUserFavoriteJobsWithTraditionalPaging(request);
        }
    }
    
    /**
     * 使用游标分页获取用户收藏的职位列表
     */
    private CursorResponseDto<JobDetailOutputDto> getUserFavoriteJobsWithCursor(JobListInputDto request) {
        System.out.println("【性能日志】使用游标分页获取用户收藏职位列表");
        long startTime = System.currentTimeMillis();
        
        // 获取当前用户ID
        Integer userId = UserContextHolder.getUid();
        if (userId == null) {
            // 用户未登录，返回空结果
            System.out.println("【性能日志】用户未登录，返回空收藏列表");
            return new CursorResponseDto<>(
                    new ArrayList<>(),
                    null,
                    null,
                    false,
                    false,
                    0L
            );
        }
        
        // 默认每页大小，如果未指定
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(3); // 保持与前端一致的默认值
        }
        
        // 解析游标
        String maxCursor = request.getMaxCursor();
        String minCursor = request.getMinCursor();
        
        boolean isForward = StringUtils.hasText(maxCursor); // 向前查询（历史内容）
        boolean isBackward = StringUtils.hasText(minCursor); // 向后查询（新内容）
        
        if (isForward && isBackward) {
            // 不能同时指定两个方向，以maxCursor为优先
            isBackward = false;
        }
        
        // 1. 查询用户收藏的职位IDs
        LambdaQueryWrapper<JobFavorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(JobFavorite::getUserId, userId)
                     .eq(JobFavorite::getStatus, 1); // 只查询有效收藏
        
        // 获取收藏列表
        List<JobFavorite> favorites = this.list(favoriteWrapper);
        
        if (favorites.isEmpty()) {
            System.out.println("【性能日志】用户没有收藏职位，返回空列表");
            return new CursorResponseDto<>(
                    new ArrayList<>(),
                    maxCursor,
                    minCursor,
                    isForward ? false : true,
                    isBackward ? false : true,
                    0L
            );
        }
        
        // 提取收藏的职位ID
        List<Integer> favoriteJobIds = favorites.stream()
                .map(JobFavorite::getJobId)
                .collect(Collectors.toList());
        
        // 2. 查询这些职位的详细信息，带游标分页
        LambdaQueryWrapper<Jobs> jobsWrapper = new LambdaQueryWrapper<>();
        jobsWrapper.in(Jobs::getId, favoriteJobIds);
        
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
                    jobsWrapper.and(w -> w
                            .lt(Jobs::getCreatedAt, forwardTimestamp)
                            .or(o -> o
                                    .eq(Jobs::getCreatedAt, forwardTimestamp)
                                    .lt(Jobs::getId, forwardId)
                            )
                    );
                }
            }
            // 按时间降序，同一时间按ID降序
            jobsWrapper.orderByDesc(Jobs::getCreatedAt, Jobs::getId);
            
        } else if (isBackward) {
            // 解析minCursor (向右滑，获取更新内容)
            Map<String, Object> minCursorValues = CursorUtils.decodeCursor(minCursor);
            if (minCursorValues != null) {
                final Date backwardTimestamp = (Date) minCursorValues.get("timestamp");
                final Integer backwardId = (Integer) minCursorValues.get("id");
                
                // 构建查询条件：获取比当前游标更新的内容
                if (backwardTimestamp != null && backwardId != null) {
                    // 时间比游标新或时间相同但ID更大
                    jobsWrapper.and(w -> w
                            .gt(Jobs::getCreatedAt, backwardTimestamp)
                            .or(o -> o
                                    .eq(Jobs::getCreatedAt, backwardTimestamp)
                                    .gt(Jobs::getId, backwardId)
                            )
                    );
                }
            }
            // 按时间升序，同一时间按ID升序（获取后需要反转）
            jobsWrapper.orderByAsc(Jobs::getCreatedAt, Jobs::getId);
            
        } else {
            // 没有指定游标，获取最新内容
            jobsWrapper.orderByDesc(Jobs::getCreatedAt, Jobs::getId);
        }
        
        // 查询数据
        Integer limit = request.getPageSize() + 1; // 多查一条用于判断是否有更多数据
        List<Jobs> jobs = jobsService.list(jobsWrapper.last("LIMIT " + limit));
        
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
            }
            
            // 获取新内容方向的下一个游标（第一条记录）
            Jobs firstJob = jobs.get(0);
            nextMinCursor = CursorUtils.encodeCursor(firstJob.getCreatedAt(), firstJob.getId());
        }
        
        // 3. 批量获取所需的数据
        
        // 3.1 收集所有需要的用户ID
        List<Integer> userIds = jobs.stream()
            .map(Jobs::getUserId)
            .distinct()
            .collect(Collectors.toList());
            
        // 3.2 批量查询用户信息
        long userQueryStart = System.currentTimeMillis();
        List<Users> users = new ArrayList<>();
        if (!userIds.isEmpty()) {
            users = jobsService.getUsersService().listByIds(userIds);
        }
        
        // 用户ID到用户对象的映射
        Map<Integer, Users> userMap = users.stream()
            .collect(Collectors.toMap(Users::getId, user -> user, (u1, u2) -> u1));
            
        // 3.3 批量查询统计数据
        
        // 所有职位ID列表
        List<Integer> jobIds = jobs.stream()
            .map(Jobs::getId)
            .collect(Collectors.toList());
            
        // 获取评论计数
        Map<Integer, Integer> commentsCountMap = new HashMap<>();
        try {
            // 批量获取评论数
            for (Integer jobId : jobIds) {
                int commentsCount = jobsService.getJobCommentsService().getJobCommentsCount(jobId);
                commentsCountMap.put(jobId, commentsCount);
            }
        } catch (Exception e) {
            System.out.println("批量获取评论数失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> commentsCountMap.put(id, 0));
        }
        
        // 获取收藏计数（可以直接构建，因为我们知道这些都是被收藏的）
        Map<Integer, Integer> favoritesCountMap = new HashMap<>();
        for (Integer jobId : jobIds) {
            favoritesCountMap.put(jobId, 1); // 至少被当前用户收藏过
        }
        
        // 获取点赞计数
        Map<Integer, Integer> likesCountMap = new HashMap<>();
        try {
            // 批量获取点赞数
            for (Integer jobId : jobIds) {
                int likesCount = jobsService.getJobLikesService().getJobLikesCount(jobId);
                likesCountMap.put(jobId, likesCount);
            }
        } catch (Exception e) {
            System.out.println("批量获取点赞数失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> likesCountMap.put(id, 0));
        }
        
        // 由于这是收藏列表，我们已经知道所有职位都是被收藏的
        final Map<Integer, Boolean> favoriteMap = new HashMap<>();
        for (Integer jobId : jobIds) {
            favoriteMap.put(jobId, true);
        }
        
        // 批量查询点赞状态
        final Map<Integer, Boolean> likeMap = new HashMap<>();
        try {
            Map<Integer, Boolean> tempLikeMap = jobsService.getJobLikesService().batchGetLikeStatus(jobIds, userId);
            likeMap.putAll(tempLikeMap);
        } catch (Exception e) {
            System.out.println("批量获取点赞状态失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> likeMap.put(id, false));
        }
        
        // 4. 批量转换为DTO
        List<JobDetailOutputDto> result = new ArrayList<>();
        
        for (Jobs job : jobs) {
            // 使用优化的批量转换方法
            JobDetailOutputDto dto = jobsService.convertDto(
                job, 
                userMap, 
                likesCountMap, 
                favoritesCountMap, 
                commentsCountMap, 
                favoriteMap, 
                likeMap
            );
            
            // 为每个DTO设置游标值，这样前端可以直接使用它来加载更多
            String cursor = CursorUtils.encodeCursor(job.getCreatedAt(), job.getId());
            dto.setCursor(cursor);
            System.out.println("【DTO游标设置】为记录 " + job.getId() + " 设置游标: " + cursor);
            
            result.add(dto);
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】游标分页获取用户收藏职位完成 - 耗时: " + (endTime - startTime) + "ms");
        
        // 更新缓存和状态变量，以便前端可以通过getCachedUserFavorites和getUserFavoritesLoadingState获取
        this._cachedUserFavorites = result;
        this._userFavoritesMaxCursor = (!isForward && !isBackward && !jobs.isEmpty()) || (isForward && hasMore) ? nextMaxCursor : maxCursor;
        this._userFavoritesMinCursor = (!isForward && !isBackward && !jobs.isEmpty()) || (isBackward && hasMore) ? nextMinCursor : minCursor;
        this._hasMoreUserFavorites = isForward ? hasMore : true;
        this._hasMoreNewUserFavorites = isBackward ? hasMore : false;
        
        // 打印游标调试信息
        System.out.println("【收藏游标】当前设置的游标值 - nextMaxCursor: " + this._userFavoritesMaxCursor + 
                          ", nextMinCursor: " + this._userFavoritesMinCursor + 
                          ", 请求方向: " + (isForward ? "前向(历史)" : isBackward ? "后向(新内容)" : "初始加载"));
        
        return new CursorResponseDto<>(
                result,
                (!isForward && !isBackward && !jobs.isEmpty()) || (isForward && hasMore) ? nextMaxCursor : maxCursor,
                (!isForward && !isBackward && !jobs.isEmpty()) || (isBackward && hasMore) ? nextMinCursor : minCursor,
                isForward ? hasMore : true, // 历史方向是否有更多数据
                isBackward ? hasMore : false, // 新内容方向是否有更多数据（初始加载时为false，因为已加载最新数据）
                (long) result.size()
        );
    }
    
    /**
     * 使用传统分页获取用户收藏的职位列表
     */
    private Page<JobDetailOutputDto> getUserFavoriteJobsWithTraditionalPaging(JobListInputDto request) {
        // 原有的传统分页实现，复制现有的getUserFavoriteJobs方法内容
        long startTime = System.currentTimeMillis();
        
        // 获取当前用户ID
        Integer userId = UserContextHolder.getUid();
        if (userId == null) {
            // 用户未登录，返回空结果
            System.out.println("【性能日志】用户未登录，返回空收藏列表");
            Page<Jobs> emptyPage = PageUtil.build(request);
            return PageUtil.build(emptyPage, new ArrayList<>());
        }
        
        // 创建分页对象
        Page<Jobs> page = PageUtil.build(request);
        
        // 1. 查询用户收藏的所有有效职位ID
        long favoriteQueryStart = System.currentTimeMillis();
        Page<JobFavorite> favoritePage = new Page<>(1, request.getPageSize());
        List<JobFavorite> favorites = this.lambdaQuery()
            .eq(JobFavorite::getUserId, userId)
            .eq(JobFavorite::getStatus, 1) // 只查询有效收藏
            .page(favoritePage)
            .getRecords();
            
        if (favorites.isEmpty()) {
            System.out.println("【性能日志】用户没有收藏职位，返回空列表");
            return PageUtil.build(page, new ArrayList<>());
        }
        
        // 提取收藏的职位ID
        List<Integer> favoriteJobIds = favorites.stream()
            .map(JobFavorite::getJobId)
            .collect(Collectors.toList());
            
        long favoriteQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】查询用户收藏职位ID耗时: " + (favoriteQueryEnd - favoriteQueryStart) + "ms, 收藏数量: " + favoriteJobIds.size());
        
        // 2. 批量查询这些职位的详细信息
        long jobsQueryStart = System.currentTimeMillis();
        List<Jobs> favoriteJobs = jobsService.lambdaQuery()
            .in(Jobs::getId, favoriteJobIds)
            .list();
            
        if (favoriteJobs.isEmpty()) {
            System.out.println("【性能日志】没有找到有效的职位信息，返回空列表");
            return PageUtil.build(page, new ArrayList<>());
        }
        
        // 维护ID到索引的映射，以保持收藏顺序
        Map<Integer, Integer> jobIdToIndexMap = new HashMap<>();
        for (int i = 0; i < favoriteJobIds.size(); i++) {
            jobIdToIndexMap.put(favoriteJobIds.get(i), i);
        }
        
        // 按收藏顺序排序职位
        favoriteJobs.sort((job1, job2) -> {
            Integer index1 = jobIdToIndexMap.get(job1.getId());
            Integer index2 = jobIdToIndexMap.get(job2.getId());
            return index1.compareTo(index2);
        });
        
        long jobsQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】批量查询收藏职位详情耗时: " + (jobsQueryEnd - jobsQueryStart) + "ms, 职位数量: " + favoriteJobs.size());
        
        // 3. 批量获取所需的数据，避免单个查询
        
        // 3.1 收集所有需要的用户ID
        List<Integer> userIds = favoriteJobs.stream()
            .map(Jobs::getUserId)
            .distinct()
            .collect(Collectors.toList());
            
        // 3.2 批量查询用户信息
        long userQueryStart = System.currentTimeMillis();
        List<Users> users = new ArrayList<>();
        if (!userIds.isEmpty()) {
            users = jobsService.getUsersService().lambdaQuery()
                .in(Users::getId, userIds)
                .list();
        }
        
        // 用户ID到用户对象的映射
        Map<Integer, Users> userMap = users.stream()
            .collect(Collectors.toMap(Users::getId, user -> user, (u1, u2) -> u1));
            
        long userQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】批量查询用户信息耗时: " + (userQueryEnd - userQueryStart) + "ms, 用户数量: " + users.size());
        
        // 3.3 批量查询统计数据
        long statsQueryStart = System.currentTimeMillis();
        
        // 所有职位ID列表
        List<Integer> jobIds = favoriteJobs.stream()
            .map(Jobs::getId)
            .collect(Collectors.toList());
            
        // 获取评论计数
        Map<Integer, Integer> commentsCountMap = new HashMap<>();
        try {
            // 逐个获取评论数
            for (Integer jobId : jobIds) {
                int commentsCount = jobsService.getJobCommentsService().getJobCommentsCount(jobId);
                commentsCountMap.put(jobId, commentsCount);
            }
        } catch (Exception e) {
            System.out.println("批量获取评论数失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> commentsCountMap.put(id, 0));
        }
        
        // 获取收藏计数（可以直接构建，因为我们知道这些都是被收藏的）
        Map<Integer, Integer> favoritesCountMap = new HashMap<>();
        for (Integer jobId : jobIds) {
            favoritesCountMap.put(jobId, 1); // 至少被当前用户收藏过
        }
        
        // 获取点赞计数
        Map<Integer, Integer> likesCountMap = new HashMap<>();
        try {
            // 逐个获取点赞数
            for (Integer jobId : jobIds) {
                int likesCount = jobsService.getJobLikesService().getJobLikesCount(jobId);
                likesCountMap.put(jobId, likesCount);
            }
        } catch (Exception e) {
            System.out.println("批量获取点赞数失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> likesCountMap.put(id, 0));
        }
        
        long statsQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】批量查询统计数据耗时: " + (statsQueryEnd - statsQueryStart) + "ms");
        
        // 3.4 批量查询点赞和收藏状态
        // 由于这是收藏列表，我们已经知道所有职位都是被收藏的
        final Map<Integer, Boolean> favoriteMap = new HashMap<>();
        for (Integer jobId : jobIds) {
            favoriteMap.put(jobId, true);
        }
        
        // 批量查询点赞状态
        final Map<Integer, Boolean> likeMap = new HashMap<>();
        try {
            Map<Integer, Boolean> tempLikeMap = jobsService.getJobLikesService().batchGetLikeStatus(jobIds, userId);
            likeMap.putAll(tempLikeMap);
        } catch (Exception e) {
            System.out.println("批量获取点赞状态失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> likeMap.put(id, false));
        }
        
        // 4. 批量转换为DTO
        long dtoConvertStart = System.currentTimeMillis();
        List<JobDetailOutputDto> result = new ArrayList<>();
        
        for (Jobs job : favoriteJobs) {
            // 使用优化的批量转换方法
            JobDetailOutputDto dto = jobsService.convertDto(
                job, 
                userMap, 
                likesCountMap, 
                favoritesCountMap, 
                commentsCountMap, 
                favoriteMap, 
                likeMap
            );
            
            // 为每个DTO设置游标值，这样前端可以直接使用它来加载更多
            String cursor = CursorUtils.encodeCursor(job.getCreatedAt(), job.getId());
            dto.setCursor(cursor);
            System.out.println("【DTO游标设置】为记录 " + job.getId() + " 设置游标: " + cursor);
            
            result.add(dto);
        }
        
        long dtoConvertEnd = System.currentTimeMillis();
        System.out.println("【性能日志】优化后的DTO批量转换耗时: " + (dtoConvertEnd - dtoConvertStart) + "ms");
        
        // 5. 构建分页结果
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】获取用户收藏职位完成 - 总耗时: " + (endTime - startTime) + "ms");
        
        return PageUtil.build(page, result);
    }
    
    /**
     * 收藏或者取消收藏
     */
    public void addOrRemoveFavorite(JobFavoriteInputDto dto) {
        Integer uid = UserContextHolder.getUid();
        
        // 验证职位是否存在 (可选，如果业务需要)
        Jobs job = jobsService.getById(dto.getJobId());
        if (job == null) {
            System.out.println("职位不存在，职位ID: " + dto.getJobId());
            return;
        }
        
        // 直接使用一条SQL完成插入或更新，实现原子操作
        int status = Objects.equals(dto.getIsFavorite(), 1) ? 1 : 0;
        int affected = this.baseMapper.insertOrUpdateFavoriteStatus(dto.getJobId(), uid, status);
        
        // 记录操作结果
        String actionName = status == 1 ? "收藏" : "取消收藏";
        System.out.println(actionName + "操作完成，职位ID: " + dto.getJobId() + 
                           ", 用户ID: " + uid + 
                           ", 状态: " + status +
                           ", 影响行数: " + affected);
    }
    
    /**
     * 判断职位是否被当前用户收藏
     */
    public boolean isJobFavorite(Integer jobId) {
        // 获取当前用户ID
        Integer uid = UserContextHolder.getUid();
        
        // 如果有用户ID，检查该用户是否收藏了此职位
        if (Objects.nonNull(uid)) {
            return this.lambdaQuery()
                .eq(JobFavorite::getJobId, jobId)
                .eq(JobFavorite::getUserId, uid)
                .eq(JobFavorite::getStatus, 1) // 只检查有效收藏
                .exists();
        }
        
        return false;
    }
    
    /**
     * 获取职位收藏数量
     */
    public int getJobFavoritesCount(Integer jobId) {
        return this.lambdaQuery()
            .eq(JobFavorite::getJobId, jobId)
            .eq(JobFavorite::getStatus, 1) // 只计算有效收藏
            .count().intValue();
    }

    // 缓存用户的收藏列表，用于支持游标分页
    private List<JobDetailOutputDto> _cachedUserFavorites = new ArrayList<>();
    
    // 用户收藏游标
    private String _userFavoritesMaxCursor; // 历史方向游标 (向下加载更多历史收藏)
    private String _userFavoritesMinCursor; // 新内容方向游标 (向上加载更新收藏)
    
    // 是否有更多内容可加载
    private boolean _hasMoreUserFavorites = true;
    private boolean _hasMoreNewUserFavorites = false;
    
    /**
     * 获取缓存的用户收藏列表
     * 用于前端在调用getFavoriteJobs后获取完整的缓存列表
     */
    public List<JobDetailOutputDto> getCachedUserFavorites() {
        return _cachedUserFavorites;
    }
    
    /**
     * 获取用户收藏加载状态
     * 返回包含各种加载状态标志的Map
     */
    public Map<String, Object> getUserFavoritesLoadingState() {
        Map<String, Object> state = new HashMap<>();
        state.put("hasMore", _hasMoreUserFavorites);
        state.put("hasMoreNew", _hasMoreNewUserFavorites);
        state.put("cachedFavoritesCount", _cachedUserFavorites.size());
        state.put("maxCursor", _userFavoritesMaxCursor);
        state.put("minCursor", _userFavoritesMinCursor);
        return state;
    }

    // 添加批量查询收藏状态的方法
    public Map<Integer, Boolean> batchGetFavoriteStatus(List<Integer> jobIds, Integer userId) {
        long startTime = System.currentTimeMillis();
        System.out.println("【性能日志】开始批量查询收藏状态 - 职位数量: " + jobIds.size());
        
        Map<Integer, Boolean> result = new HashMap<>();
        
        // 初始化默认状态为false
        for (Integer jobId : jobIds) {
            result.put(jobId, false);
        }
        
        if (jobIds.isEmpty() || userId == null) {
            return result;
        }
        
        try {
            // 批量查询所有有效收藏的记录
            List<JobFavorite> favoriteList = this.lambdaQuery()
                .in(JobFavorite::getJobId, jobIds)
                .eq(JobFavorite::getUserId, userId)
                .eq(JobFavorite::getStatus, 1) // 只查询有效收藏
                .list();
            
            // 更新收藏状态
            for (JobFavorite favorite : favoriteList) {
                result.put(favorite.getJobId(), true);
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("【性能日志】批量查询收藏状态完成 - 耗时: " + (endTime - startTime) + 
                    "ms, 已收藏数量: " + favoriteList.size() + "/" + jobIds.size());
            
            return result;
        } catch (Exception e) {
            System.out.println("【性能日志】批量查询收藏状态异常: " + e.getMessage());
            return result;
        }
    }
}
