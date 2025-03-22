package com.ideafly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ideafly.model.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

}
