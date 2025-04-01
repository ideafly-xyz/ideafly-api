package com.ideafly.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobComments {
    private Integer id; // 评论ID
    private Integer jobId; // 职位ID (关联 jobs 表)
    private Integer userId; // 评论用户ID (关联用户表)
    private Integer parentCommentId; // 父级评论ID (用于实现评论回复)
    private String content; // 评论内容
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
