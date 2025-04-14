package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_likes")
public class JobLikes {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer jobId;
    private Integer userId;
    private LocalDateTime createdAt; // 收藏时间
}
