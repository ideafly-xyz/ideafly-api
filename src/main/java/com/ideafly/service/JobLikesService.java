package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.dto.job.JobLikeInputDto;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.mapper.JobLikesMapper;
import com.ideafly.model.JobFavorite;
import com.ideafly.model.JobLikes;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class JobLikesService extends ServiceImpl<JobLikesMapper, JobLikes> {
    /**
     * 收藏或者取消收藏
     */
    public void addOrRemoveLike(JobLikeInputDto dto) {
        Integer uid = UserContextHolder.getUid();
        JobLikes like = this.lambdaQuery().eq(JobLikes::getJobId, dto.getJobId()).eq(JobLikes::getUserId, uid).one();
        // 取消点赞
        if (Objects.nonNull(like) && Objects.equals(dto.getIsLike(),0) ) {
            this.removeById(like);
            return;
        }
        // 点赞
        if(Objects.isNull(like) && Objects.equals(dto.getIsLike(),1)){
            JobLikes jobLikes = new JobLikes();
            jobLikes.setUserId(uid);
            jobLikes.setJobId(dto.getJobId());
            this.save(jobLikes);
        }
    }
    public int getJobLikesCount(Integer jobId){
        return this.lambdaQuery().eq(JobLikes::getJobId, jobId).count().intValue();
    }
    public boolean isJobLike(Integer jobId){
        Integer uid = UserContextHolder.getUid();
        if (Objects.isNull(uid)) {
            return false;
        }
        return this.lambdaQuery().eq(JobLikes::getJobId, jobId).eq(JobLikes::getUserId, uid).exists();
    }
}
