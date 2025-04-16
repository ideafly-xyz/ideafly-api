package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.model.JobFavorite;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
        JobFavorite favorite = this.lambdaQuery().eq(JobFavorite::getJobId, dto.getJobId()).eq(JobFavorite::getUserId, uid).one();
        // 取消收藏
        if (Objects.nonNull(favorite) && Objects.equals(dto.getIsFavorite(),0) ) {
            this.removeById(favorite);
            jobsService.favorites(dto.getJobId(), false);
            return;
        }
        // 收藏
        if(Objects.isNull(favorite) && Objects.equals(dto.getIsFavorite(),1)){
            JobFavorite jobFavorite = new JobFavorite();
            jobFavorite.setUserId(uid);
            jobFavorite.setJobId(dto.getJobId());
            this.save(jobFavorite);
            jobsService.favorites(dto.getJobId(), true);
        }
    }
    
    /**
     * 判断职位是否被当前用户收藏
     * 修改后不再强制要求用户登录才能查看收藏状态
     */
    public boolean isJobFavorite(Integer jobId) {
        // 获取当前用户ID（可能为null）
        Integer uid = UserContextHolder.getUid();
        
        // 如果有用户ID，检查该用户是否收藏了此职位
        if (Objects.nonNull(uid)) {
            return this.lambdaQuery().eq(JobFavorite::getJobId, jobId).eq(JobFavorite::getUserId, uid).exists();
        }
        
        // 即使没有用户ID，仍然返回该职位是否有收藏记录
        // 根据职位收藏数判断，如果有收藏数则认为是已收藏
        int favoriteCount = this.lambdaQuery().eq(JobFavorite::getJobId, jobId).count().intValue();
        return favoriteCount > 0;
    }
}
