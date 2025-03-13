package com.ideafly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideafly.model.Jobs;
import com.ideafly.model.Users;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobsMapper extends BaseMapper<Jobs> {
}
