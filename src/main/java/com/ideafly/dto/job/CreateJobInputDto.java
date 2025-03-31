package com.ideafly.dto.job;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateJobInputDto {

    @NotBlank(message = "帖子标题不能为空") //  使用 Validation 注解进行参数校验
    private String title;
    private String content;
    @NotBlank(message = "联系方式不能为空")
    private String contactInfo;
    private Integer recruitmentType;
    private Integer profession;
    private Integer workType;
    private Integer city;
    private Integer industryDomain;

}