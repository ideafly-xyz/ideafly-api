package com.ideafly.dto.job;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobDetailOutputDto {
    private Integer id;
    private Integer userId;
    private String postTitle;
    private String postContent;
    private String contactInfo;

    private String recruitmentTypeName;
    private String professionName;
    private String workTypeName;
    private String cityName;
    private String industryDomainName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}