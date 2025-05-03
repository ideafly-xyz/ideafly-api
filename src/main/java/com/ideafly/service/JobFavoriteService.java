package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.dto.job.JobListInputDto;
import com.ideafly.dto.job.JobDetailOutputDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.model.JobFavorite;
import com.ideafly.model.Jobs;
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
public class JobFavoriteService extends ServiceImpl<JobFavoriteMapper, JobFavorite> {

    @Resource
    private JobsService jobsService;
    
    /**
     * 获取用户收藏的职位列表
     */
    public Page<JobDetailOutputDto> getUserFavoriteJobs(JobListInputDto request) {
        long startTime = System.currentTimeMillis();
        System.out.println("【性能日志】开始获取用户收藏职位列表 - 参数: " + request);
        
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
        Page<JobFavorite> favoritePage = new Page<>(request.getPageNum(), request.getPageSize());
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
            .orderByDesc(Jobs::getId) // 按ID降序，最新收藏的在前面
            .list();
            
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
        
        // 3. 转换为DTO并添加isFavorite标记
        long dtoConvertStart = System.currentTimeMillis();
        List<JobDetailOutputDto> result = new ArrayList<>();
        
        for (Jobs job : favoriteJobs) {
            JobDetailOutputDto dto = jobsService.convertDto(job);
            dto.setIsFavorite(true); // 设置为已收藏
            result.add(dto);
        }
        
        long dtoConvertEnd = System.currentTimeMillis();
        System.out.println("【性能日志】DTO转换耗时: " + (dtoConvertEnd - dtoConvertStart) + "ms");
        
        // 4. 构建分页结果
        long endTime = System.currentTimeMillis();
        System.out.println("【性能日志】获取用户收藏职位完成 - 总耗时: " + (endTime - startTime) + "ms");
        
        return PageUtil.build(page, result);
    }
    
    /**
     * 收藏或者取消收藏
     */
    public void addOrRemoveFavorite(JobFavoriteInputDto dto) {
        Integer uid = UserContextHolder.getUid();
        
        // 查找是否存在收藏记录
        JobFavorite favorite = this.lambdaQuery()
            .eq(JobFavorite::getJobId, dto.getJobId())
            .eq(JobFavorite::getUserId, uid)
            .one();
            
        // 判断操作类型
        if (Objects.equals(dto.getIsFavorite(), 1)) {
            // 添加收藏
            if (Objects.isNull(favorite)) {
                // 无记录，创建新记录
                JobFavorite jobFavorite = new JobFavorite();
                jobFavorite.setUserId(uid);
                jobFavorite.setJobId(dto.getJobId());
                jobFavorite.setStatus(1); // 有效状态
                this.save(jobFavorite);
            } else if (favorite.getStatus() == 0) {
                // 有记录但已取消，更新状态
                favorite.setStatus(1);
                this.updateById(favorite);
            }
            // 已经收藏的不做处理
        } else {
            // 取消收藏
            if (Objects.nonNull(favorite) && favorite.getStatus() == 1) {
                // 更新状态为取消
                favorite.setStatus(0);
                this.updateById(favorite);
            }
            // 不存在或已取消的不做处理
        }
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
