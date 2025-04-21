package com.ideafly.dto.job;

import com.ideafly.dto.PageBaseInputDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class JobListInputDto extends PageBaseInputDto {
    // 保留为空类，仅继承分页功能
}