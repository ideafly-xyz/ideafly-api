package com.ideafly.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "评论列表请求DTO - 游标分页")
public class JobCommentPageDto {
    
    @Schema(description = "职位ID")
    private Integer jobId;
    
    @Schema(description = "每页大小", defaultValue = "10")
    private Integer pageSize = 10;
    
    @Schema(description = "最大游标值 (用于加载更早的评论)", required = false)
    private String maxCursor;
    
    @Schema(description = "是否是预加载请求", defaultValue = "false")
    private Boolean preload = false;
} 