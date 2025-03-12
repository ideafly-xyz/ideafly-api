package com.ideafly.dto.user;

import lombok.Data;

@Data
public class UserDto {
    // 用户ID，主键，自增
    private Integer id;
    private String mobile;
    private String username;
    private String nickname;
}
