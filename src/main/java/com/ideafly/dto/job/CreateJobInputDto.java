package com.ideafly.dto.job;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateJobInputDto {

    @NotBlank(message = "帖子标题不能为空") //  使用 Validation 注解进行参数校验
    private String postTitle;
    
    @NotBlank(message = "帖子内容不能为空")
    private String postContent;
}