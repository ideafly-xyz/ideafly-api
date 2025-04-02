package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobFavorite {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 收藏ID
    private Integer jobId; // 职位ID (关联 jobs 表)
    private Integer userId; // 收藏用户ID (关联用户表)
    private LocalDateTime createdAt; // 收藏时间
}
