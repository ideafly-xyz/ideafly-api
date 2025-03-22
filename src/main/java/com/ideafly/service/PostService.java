package com.ideafly.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.mapper.PostMapper;
import com.ideafly.model.Post;
import org.springframework.stereotype.Service;

@Service
public class PostService extends ServiceImpl<PostMapper, Post> {
    public IPage<Post> getPostList(Integer currentPage, Integer pageSize) {
        Page<Post> page = new Page<>(currentPage, pageSize);
        return this.lambdaQuery().orderByDesc(Post::getLikes).page(page);
    }
}