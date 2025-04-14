package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class JobComments {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 评论ID
    private Integer jobId; // 职位ID (关联 jobs 表)
    private Integer userId; // 评论用户ID (关联用户表)
    private Integer parentCommentId; // 父级评论ID (用于实现评论回复)
    private String content; // 评论内容
    private LocalDateTime createdAt; // 创建时间
    @TableField(exist = false)
    private List<JobComments> children = new ArrayList<>();
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String userAvatar;
}
