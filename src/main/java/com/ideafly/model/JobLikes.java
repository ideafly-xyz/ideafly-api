package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobLikes {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer jobId;
    private Integer userId;
    private LocalDateTime createdAt; // 收藏时间
}
