package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.model.JobFavorite;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class JobFavoriteService extends ServiceImpl<JobFavoriteMapper, JobFavorite> {

    /**
     * 收藏或者取消收藏
     */
    public void addOrRemoveFavorite(JobFavoriteInputDto dto) {
        Integer uid = UserContextHolder.getUid();
        JobFavorite favorite = this.lambdaQuery().eq(JobFavorite::getJobId, dto.getJobId()).eq(JobFavorite::getUserId, uid).one();
        // 取消收藏
        if (Objects.nonNull(favorite) && Objects.equals(dto.getIsFavorite(),0) ) {
            this.removeById(favorite);
            return;
        }
        // 收藏
        if(Objects.isNull(favorite) && Objects.equals(dto.getIsFavorite(),0)){
            JobFavorite jobFavorite = new JobFavorite();
            jobFavorite.setUserId(uid);
            jobFavorite.setJobId(dto.getJobId());
            this.save(jobFavorite);
        }
    }
}
