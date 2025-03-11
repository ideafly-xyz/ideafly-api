package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 用户ID，主键，自增
    private String username; // 用户名，唯一
    private String email; // 邮箱，唯一，用于登录和找回密码
    private String mobile; // 手机号，唯一，用于登录和短信通知
    private String passwordHash; // 密码哈希值，存储加密后的密码
    private String avatarUrl; // 头像URL
    private String nickname; // 昵称
    private String realName; // 真实姓名
    private Integer gender; // 性别，枚举类型：male (男), female (女), other (其他)
    private String location; // 所在地
    private String personalBio; // 个人简介
    private String websiteUrl; // 个人网站URL
    private String wechatId; // 微信号，唯一
    private String githubId; // GitHub ID，唯一
    private LocalDateTime registrationTime; // 注册时间，默认当前时间戳
    private LocalDateTime lastLoginTime; // 最后登录时间
    private Integer accountStatus; // 账号状态，枚举类型：1 (激活), 2 (未激活), 3 (封禁), 0 (已删除)，默认 1
    private LocalDateTime createdAt; // 创建时间，默认当前时间戳
    private LocalDateTime updatedAt; // 更新时间，自动更新为最新时间戳
}