package com.ideafly.mapper.interact;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideafly.model.interact.JobLikes;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JobLikesMapper extends BaseMapper<JobLikes> {
    
    /**
     * 插入或更新点赞状态，实现原子操作
     * 使用 INSERT ON DUPLICATE KEY UPDATE 实现
     */
    @Insert("INSERT INTO job_likes(job_id, user_id, status) VALUES (#{jobId}, #{userId}, #{status}) " +
           "ON DUPLICATE KEY UPDATE status = #{status}")
    int insertOrUpdateLikeStatus(@Param("jobId") Integer jobId, 
                               @Param("userId") String userId, 
                               @Param("status") Integer status);
}
