package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jobs")
public class Jobs {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String userId;

    private String postTitle;
    private String postContent;
    
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}