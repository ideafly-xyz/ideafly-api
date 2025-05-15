package com.ideafly.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JobLoadMoreChildrenDto {
    @NotNull(message = "职位ID不能为空")
    @Schema(description = "职位ID")
    private Integer jobId;
    
    @NotNull(message = "父评论ID不能为空")
    @Schema(description = "父评论ID")
    private Integer parentId;
    
    @Schema(description = "游标，用于分页加载")
    private String cursor;
} 