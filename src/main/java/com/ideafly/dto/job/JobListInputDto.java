package com.ideafly.dto.job;

import lombok.Data;

@Data
public class JobListInputDto {
    private Integer pageNum = 1; //  默认页码
    private Integer pageSize = 10; // 默认每页数量
    private String city;
    private String profession;
    private String recruitmentType;
    //  ...  可以根据需要添加更多查询参数 ...
}