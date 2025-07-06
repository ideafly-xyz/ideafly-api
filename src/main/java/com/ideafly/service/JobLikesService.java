package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.dto.job.JobLikeInputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.mapper.JobLikesMapper;
import com.ideafly.model.JobFavorite;
import com.ideafly.model.JobLikes;
import com.ideafly.model.Jobs;
import com.ideafly.model.Users;
import com.ideafly.utils.PageUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class JobLikesService extends ServiceImpl<JobLikesMapper, JobLikes> {
    @Resource
    private JobsService jobsService;
    
    @Resource
    private UsersService usersService;
    
    /**
     * 获取用户点赞的职位列表
     */
    public Page<JobDetailOutputDto> getUserLikedJobs(JobListInputDto request) {
        long startTime = System.currentTimeMillis();
        System.out.println("【性能日志】开始获取用户点赞职位列表 - 参数: " + request);
        
        // 获取当前用户ID
        String userId = UserContextHolder.getUid();
        if (userId == null) {
            // 用户未登录，返回空结果
            System.out.println("【性能日志】用户未登录，返回空点赞列表");
            Page<Jobs> emptyPage = PageUtil.build(request);
            return PageUtil.build(emptyPage, new ArrayList<>());
        }
        
        // 创建分页对象
        Page<Jobs> page = PageUtil.build(request);
        
        // 1. 查询用户点赞的所有有效职位ID
        long likeQueryStart = System.currentTimeMillis();
        // 总是使用第1页，因为我们已迁移到游标分页
        Page<JobLikes> likePage = new Page<>(1, request.getPageSize());
        List<JobLikes> likes = this.lambdaQuery()
            .eq(JobLikes::getUserId, userId)
            .eq(JobLikes::getStatus, 1) // 只查询有效点赞
            .page(likePage)
            .getRecords();
            
        if (likes.isEmpty()) {
            System.out.println("【性能日志】用户没有点赞职位，返回空列表");
            return PageUtil.build(page, new ArrayList<>());
        }
        
        // 提取点赞的职位ID
        List<Integer> likedJobIds = likes.stream()
            .map(JobLikes::getJobId)
            .collect(Collectors.toList());
            
        long likeQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】查询用户点赞职位ID耗时: " + (likeQueryEnd - likeQueryStart) + "ms, 点赞数量: " + likedJobIds.size());
        
        // 2. 批量查询这些职位的详细信息
        long jobsQueryStart = System.currentTimeMillis();
        List<Jobs> likedJobs = jobsService.lambdaQuery()
            .in(Jobs::getId, likedJobIds)
            .list();
            
        if (likedJobs.isEmpty()) {
            System.out.println("【性能日志】没有找到有效的职位信息，返回空列表");
            return PageUtil.build(page, new ArrayList<>());
        }
            
        // 维护ID到索引的映射，以保持点赞顺序
        Map<Integer, Integer> jobIdToIndexMap = new HashMap<>();
        for (int i = 0; i < likedJobIds.size(); i++) {
            jobIdToIndexMap.put(likedJobIds.get(i), i);
        }
        
        // 按点赞顺序排序职位
        likedJobs.sort((job1, job2) -> {
            Integer index1 = jobIdToIndexMap.get(job1.getId());
            Integer index2 = jobIdToIndexMap.get(job2.getId());
            return index1.compareTo(index2);
        });
        
        long jobsQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】批量查询点赞职位详情耗时: " + (jobsQueryEnd - jobsQueryStart) + "ms, 职位数量: " + likedJobs.size());
        
        // 3. 批量获取所需的数据，避免单个查询
        
        // 3.1 收集所有需要的用户ID
        List<String> userIds = likedJobs.stream()
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
        Map<String, Users> userMap = users.stream()
            .collect(Collectors.toMap(Users::getId, user -> user, (u1, u2) -> u1));
            
        long userQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】批量查询用户信息耗时: " + (userQueryEnd - userQueryStart) + "ms, 用户数量: " + users.size());
        
        // 3.3 批量查询统计数据
        long statsQueryStart = System.currentTimeMillis();
        
        // 所有职位ID列表
        List<Integer> jobIds = likedJobs.stream()
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
        
        // 获取收藏计数
        Map<Integer, Integer> favoritesCountMap = new HashMap<>();
        try {
            // 逐个获取收藏数
            for (Integer jobId : jobIds) {
                int favoritesCount = jobsService.getJobFavoriteService().getJobFavoritesCount(jobId);
                favoritesCountMap.put(jobId, favoritesCount);
            }
        } catch (Exception e) {
            System.out.println("批量获取收藏数失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> favoritesCountMap.put(id, 0));
        }
        
        // 获取点赞计数（可以直接构建，因为我们知道这些都是被点赞的）
        Map<Integer, Integer> likesCountMap = new HashMap<>();
        for (Integer jobId : jobIds) {
            likesCountMap.put(jobId, 1); // 至少被当前用户点赞过
        }
        
        long statsQueryEnd = System.currentTimeMillis();
        System.out.println("【性能日志】批量查询统计数据耗时: " + (statsQueryEnd - statsQueryStart) + "ms");
        
        // 3.4 批量查询点赞和收藏状态
        // 由于这是点赞列表，我们已经知道所有职位都是被点赞的
        final Map<Integer, Boolean> likeMap = new HashMap<>();
        for (Integer jobId : jobIds) {
            likeMap.put(jobId, true);
        }
        
        // 批量查询收藏状态
        final Map<Integer, Boolean> favoriteMap = new HashMap<>();
        try {
            Map<Integer, Boolean> tempFavoriteMap = jobsService.getJobFavoriteService().batchGetFavoriteStatus(jobIds, userId);
            favoriteMap.putAll(tempFavoriteMap);
        } catch (Exception e) {
            System.out.println("批量获取收藏状态失败: " + e.getMessage());
            // 设置默认值
            jobIds.forEach(id -> favoriteMap.put(id, false));
        }
        
        // 4. 批量转换为DTO
        long dtoConvertStart = System.currentTimeMillis();
        List<JobDetailOutputDto> result = new ArrayList<>();
        
        for (Jobs job : likedJobs) {
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
            result.add(dto);
        }
        
        long dtoConvertEnd = System.currentTimeMillis();
        System.out.println("【性能日志】优化后的DTO批量转换耗时: " + (dtoConvertEnd - dtoConvertStart) + "ms");
        
        // 5. 构建分页结果
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】获取用户点赞职位完成 - 总耗时: " + (endTime - startTime) + "ms");
        
        return PageUtil.build(page, result);
    }
    
    /**
     * 添加或取消点赞
     */
    public void addOrRemoveLike(JobLikeInputDto dto) {
        String uid = UserContextHolder.getUid();
        
        // 验证职位是否存在 (保留现有逻辑)
        Jobs job = jobsService.getById(dto.getJobId());
        if (job == null) {
            System.out.println("职位不存在，职位ID: " + dto.getJobId());
            return;
        }
        
        // 直接使用一条SQL完成插入或更新，实现原子操作
        int status = Objects.equals(dto.getIsLike(), 1) ? 1 : 0;
        
        // 使用Mapper中的原子操作方法
        int affected = this.baseMapper.insertOrUpdateLikeStatus(dto.getJobId(), uid, status);
        
        // 记录操作结果
        String actionName = status == 1 ? "点赞" : "取消点赞";
        System.out.println(actionName + "操作完成，职位ID: " + dto.getJobId() + 
                           ", 用户ID: " + uid + 
                           ", 状态: " + status +
                           ", 影响行数: " + affected);
    }
    
    /**
     * 获取职位点赞数量
     */
    public int getJobLikesCount(Integer jobId) {
        return this.lambdaQuery()
            .eq(JobLikes::getJobId, jobId)
            .eq(JobLikes::getStatus, 1) // 只计算有效点赞
            .count().intValue();
    }
    
    /**
     * 判断用户是否点赞了职位
     */
    public boolean isJobLikedByUser(Integer jobId, String userId) {
        // 添加调试日志
        System.out.println("检查点赞状态 - jobId: " + jobId + ", userId: " + userId);
        
        boolean result = this.lambdaQuery()
            .eq(JobLikes::getJobId, jobId)
            .eq(JobLikes::getUserId, userId)
            .eq(JobLikes::getStatus, 1) // 只检查有效点赞
            .exists();
            
        System.out.println("点赞状态查询结果: " + result);
        return result;
    }
    
    /**
     * 判断当前登录用户是否点赞了职位
     */
    public boolean isJobLike(Integer jobId) {
        String uid = UserContextHolder.getUid();
        if (Objects.nonNull(uid)) {
            return isJobLikedByUser(jobId, uid);
        }
        return false;
    }
    
    // 添加批量查询点赞状态的方法
    public Map<Integer, Boolean> batchGetLikeStatus(List<Integer> jobIds, String userId) {
        long startTime = System.currentTimeMillis();
        System.out.println("【性能日志】开始批量查询点赞状态 - 职位数量: " + jobIds.size());
        
        Map<Integer, Boolean> result = new HashMap<>();
        
        // 初始化默认状态为false
        for (Integer jobId : jobIds) {
            result.put(jobId, false);
        }
        
        if (jobIds.isEmpty() || userId == null) {
            return result;
        }
        
        try {
            // 批量查询所有已点赞的记录
            List<JobLikes> likesList = this.lambdaQuery()
                .in(JobLikes::getJobId, jobIds)
                .eq(JobLikes::getUserId, userId)
                .eq(JobLikes::getStatus, 1) // 只查询有效的点赞
                .list();
            
            // 更新点赞状态
            for (JobLikes like : likesList) {
                result.put(like.getJobId(), true);
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("【性能日志】批量查询点赞状态完成 - 耗时: " + (endTime - startTime) + 
                    "ms, 已点赞数量: " + likesList.size() + "/" + jobIds.size());
            
            return result;
        } catch (Exception e) {
            System.out.println("【性能日志】批量查询点赞状态异常: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 根据用户ID计算该用户获得的总点赞数
     * 此方法从job_likes表中动态计算，替代原先在users表中存储的totalLikes字段
     */
    public Integer calculateUserTotalLikes(String userId) {
        if (userId == null) {
            return 0;
        }
        
        try {
            // 查询用户发布的所有职位
            List<Jobs> userJobs = jobsService.lambdaQuery()
                .eq(Jobs::getUserId, userId)
                .list();
                
            if (userJobs.isEmpty()) {
                return 0;
            }
            
            // 提取所有职位ID
            List<Integer> jobIds = userJobs.stream()
                .map(Jobs::getId)
                .collect(Collectors.toList());
                
            // 统计这些职位获得的有效点赞数（status=1）
            Integer totalLikes = this.lambdaQuery()
                .in(JobLikes::getJobId, jobIds)
                .eq(JobLikes::getStatus, 1)
                .count().intValue();
                
            return totalLikes;
        } catch (Exception e) {
            System.out.println("计算用户总点赞数失败: " + e.getMessage());
            return 0;
        }
    }
}
