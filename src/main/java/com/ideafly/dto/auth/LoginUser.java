package com.ideafly.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户信息数据传输对象
 */
@Data
@NoArgsConstructor
public class LoginUser {
    private String id;
    private String username;
    private String mobile;
    private String email;
    private String avatar;
    private Integer role;
} 