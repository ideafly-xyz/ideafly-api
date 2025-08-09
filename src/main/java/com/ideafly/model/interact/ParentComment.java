package com.ideafly.model.interact;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("post_comments") // 仍然使用同一张表
public class ParentComment {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 评论ID
    private Integer jobId; // 职位ID (关联 jobs 表)
    private String userId; // 评论用户ID (关联用户表)
    private Integer parentCommentId; // 父级评论ID (应该为0)
    private Integer replyToCommentId; // 回复的评论ID (通常为null或0)
    private String content; // 评论内容
    private LocalDateTime createdAt; // 创建时间
    
    // 非数据库字段，用于前端展示
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String userAvatar;
    
    // 子评论相关
    @TableField(exist = false)
    private List<ChildComment> children = new ArrayList<>();
    @TableField(exist = false)
    private Boolean hasMoreChildren = false; // 是否有更多子评论
    @TableField(exist = false)
    private String childrenNextCursor; // 子评论的下一页游标
    @TableField(exist = false)
    private Integer childrenCount = 0; // 子评论总数
} 