package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.model.JobFavorite;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class JobFavoriteService extends ServiceImpl<JobFavoriteMapper, JobFavorite> {

    @Resource
    private JobsService jobsService;
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
                jobsService.favorites(dto.getJobId(), true);
            } else if (favorite.getStatus() == 0) {
                // 有记录但已取消，更新状态
                favorite.setStatus(1);
                this.updateById(favorite);
                jobsService.favorites(dto.getJobId(), true);
            }
            // 已经收藏的不做处理
        } else {
            // 取消收藏
            if (Objects.nonNull(favorite) && favorite.getStatus() == 1) {
                // 更新状态为取消
                favorite.setStatus(0);
                this.updateById(favorite);
                jobsService.favorites(dto.getJobId(), false);
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
