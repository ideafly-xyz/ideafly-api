package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_favorites")
public class JobFavorite {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 收藏ID
    private Integer jobId; // 职位ID (关联 jobs 表)
    private String userId; // 收藏用户ID (关联用户表)
    private Integer status; // 状态 (1: 有效, 0: 无效)
    private LocalDateTime createdAt; // 收藏时间
}
