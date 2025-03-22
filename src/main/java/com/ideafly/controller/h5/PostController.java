package com.ideafly.controller.h5;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ideafly.common.R;
import com.ideafly.model.Post;
import com.ideafly.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Tag(name = "社区帖子相关接口", description = "社区帖子信息")
@RestController
@RequestMapping("/posts")
public class PostController {
    @Resource
    private PostService postService;

    @GetMapping()
    public R<IPage<Post>> get(@RequestParam(value = "currentPage", required = true) Integer currentPage,
                              @RequestParam(value = "pageSize", required = true) Integer pageSize
                              ) {
        return R.success(postService.getPostList(currentPage, pageSize));
    }
}
