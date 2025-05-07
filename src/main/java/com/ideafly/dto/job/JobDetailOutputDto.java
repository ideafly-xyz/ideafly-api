package com.ideafly.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDetailOutputDto implements Serializable {
    private Integer id;
    private Integer userId;
    private String publisherName;
    private String publisherAvatar;
    private String postTitle;
    private String postContent;
    @Schema(description = "发布时间", name = "publish_time")
    private String publishTime;
    private LocalDateTime updatedAt;
    private Integer likes;
    private Integer favorites;
    private Integer comments;
    private Integer shares;
    private Boolean isFavorite=false; // 是否收藏
    private Boolean isLike=false;
    private String cursor; // 游标值，用于分页
}