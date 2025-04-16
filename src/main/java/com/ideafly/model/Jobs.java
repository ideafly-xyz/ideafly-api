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

    private Integer userId;

    private String postTitle;
    private String postContent;
    private String contactInfo;

    private Integer recruitmentType;


    private Integer profession;


    private Integer workType;

    private Integer city;

    private Integer industryDomain;
    private Integer likes;
    private Integer comments;
    private Integer favorites;
    private Integer shares;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}