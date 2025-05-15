package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_comments") // 仍然使用同一张表
public class ChildComment {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 评论ID
    private Integer jobId; // 职位ID (关联 jobs 表)
    private Integer userId; // 评论用户ID (关联用户表)
    private Integer parentCommentId; // 父级评论ID (用于实现评论树结构)
    private Integer replyToCommentId; // 回复的评论ID (标识回复关系)
    private String content; // 评论内容
    private LocalDateTime createdAt; // 创建时间
    
    // 非数据库字段，用于前端展示
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String userAvatar;
    @TableField(exist = false)
    private String replyToUserName; // 被回复的用户名
} 