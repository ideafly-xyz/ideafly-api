package com.ideafly.dto.user;

import lombok.Data;

@Data
public class UserGetOutputDto {
    private String id;
    private String username;
    private String email;
    private String mobile;
    private String avatar;
    private String personalBio;
    private String location;
    private Integer gender;
    private Integer totalLikes;
    private Integer followersCount;
    private Integer followingCount;
    private Integer mutualFollowCount;
} 