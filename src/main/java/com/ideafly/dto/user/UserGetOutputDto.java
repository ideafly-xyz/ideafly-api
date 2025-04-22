package com.ideafly.dto.user;

import lombok.Data;

@Data
public class UserGetOutputDto {
    private Integer id;
    private String username;
    private String email;
    private String mobile;
    private String avatar;
    private String bio;
    private Integer totalLikes;
} 