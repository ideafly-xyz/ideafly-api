package com.ideafly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideafly.model.JobFavorite;
import com.ideafly.model.JobLikes;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobLikesMapper extends BaseMapper<JobLikes> {
}
