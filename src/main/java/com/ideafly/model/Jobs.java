package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Jobs {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String title;
    private String content;
    private String salary;
    private String contactInfo;

    private Integer recruitmentType;


    private Integer profession;


    private Integer workType;

    private Integer city;

    private Integer industryDomain;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}