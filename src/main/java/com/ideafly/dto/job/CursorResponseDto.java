package com.ideafly.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "基于游标的分页响应")
public class CursorResponseDto<T> implements Serializable {
    
    @Schema(description = "数据列表")
    private List<T> records;
    
    @Schema(description = "下一个最大游标，用于获取历史内容")
    private String nextMaxCursor;
    
    @Schema(description = "下一个最小游标，用于获取最新内容")
    private String nextMinCursor;
    
    @Schema(description = "是否还有更多历史内容")
    private Boolean hasMoreHistory;
    
    @Schema(description = "是否还有更多新内容")
    private Boolean hasMoreNew;
    
    @Schema(description = "总记录数")
    private Long total;
    
    public CursorResponseDto() {
    }
    
    public CursorResponseDto(List<T> records, String nextMaxCursor, String nextMinCursor, Boolean hasMoreHistory, Boolean hasMoreNew, Long total) {
        this.records = records;
        this.nextMaxCursor = nextMaxCursor;
        this.nextMinCursor = nextMinCursor;
        this.hasMoreHistory = hasMoreHistory;
        this.hasMoreNew = hasMoreNew;
        this.total = total;
    }
} 