package com.ideafly.dto.user;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateUserInputDto {
    @Size(max = 255, message = "头像URL长度不能超过 255 个字符")
    private String avatarUrl;
    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;
    @Size(max = 50, message = "真实姓名长度不能超过 50 个字符")
    private String realName;
    private Integer gender;
    @Size(max = 100, message = "所在地长度不能超过 100 个字符")
    private String location;
    @Size(max = 1000, message = "个人简介长度不能超过 1000 个字符")
    private String personalBio;
    @Size(max = 255, message = "个人网站URL长度不能超过 255 个字符")
    private String websiteUrl;
}