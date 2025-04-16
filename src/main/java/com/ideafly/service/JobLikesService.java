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

import javax.annotation.Resource;
import java.util.Objects;

@Service
public class JobLikesService extends ServiceImpl<JobLikesMapper, JobLikes> {
    @Resource
    private JobsService jobsService;
    /**
     * 收藏或者取消收藏
     */
    public void addOrRemoveLike(JobLikeInputDto dto) {
        Integer uid = UserContextHolder.getUid();
        JobLikes like = this.lambdaQuery().eq(JobLikes::getJobId, dto.getJobId()).eq(JobLikes::getUserId, uid).one();
        // 取消点赞
        if (Objects.nonNull(like) && Objects.equals(dto.getIsLike(),0) ) {
            this.removeById(like);
            jobsService.likes(dto.getJobId(), false);
            return;
        }
        // 点赞
        if(Objects.isNull(like) && Objects.equals(dto.getIsLike(),1)){
            JobLikes jobLikes = new JobLikes();
            jobLikes.setUserId(uid);
            jobLikes.setJobId(dto.getJobId());
            this.save(jobLikes);
            jobsService.likes(dto.getJobId(), true);
        }
    }
    public int getJobLikesCount(Integer jobId){
        return this.lambdaQuery().eq(JobLikes::getJobId, jobId).count().intValue();
    }
    
    /**
     * 判断职位是否被点赞
     * 修改后不再强制要求用户登录才能查看点赞状态
     */
    public boolean isJobLike(Integer jobId){
        // 获取当前用户ID（可能为null）
        Integer uid = UserContextHolder.getUid();
        
        // 如果有用户ID，检查该用户是否点赞了此职位
        if (Objects.nonNull(uid)) {
            return this.lambdaQuery().eq(JobLikes::getJobId, jobId).eq(JobLikes::getUserId, uid).exists();
        } 
        
        // 即使没有用户ID，仍然返回该职位是否有点赞记录
        // 根据职位点赞数判断，如果有点赞数则认为是已点赞
        int likesCount = getJobLikesCount(jobId);
        return likesCount > 0;
    }
}
