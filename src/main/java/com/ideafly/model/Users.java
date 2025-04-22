package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

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
    private String avatar; // 头像URL
    private String firstName; // 名字
    private String lastName; // 姓氏
    private Integer gender; // 性别，枚举类型：male (男), female (女), other (其他)
    private String location; // 所在地
    private String bio; // 个人简介
    private String website; // 个人网站URL
    private String wechatId; // 微信号，唯一
    private String telegramId; // Telegram ID，唯一
    private String telegramUsername; // Telegram用户名
    private Integer totalLikes; // 获得的总点赞数
    private Integer status; // 账号状态，枚举类型：1 (激活), 0 (已删除)，默认 1
    private Integer role; // 用户角色，0普通用户，1管理员，默认0
    private Date createdAt; // 创建时间
    private Date updatedAt; // 更新时间
}