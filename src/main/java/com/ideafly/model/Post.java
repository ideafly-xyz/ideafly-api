package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Post {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private LocalDateTime publishTime;
    private Integer comments;
    private String publisherName;
    private String publisherAvatar;
    private String title;
    private String content;
    private String tags;
    private Integer likes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
