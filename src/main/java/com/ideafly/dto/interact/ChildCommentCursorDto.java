package com.ideafly.dto.interact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

import com.ideafly.model.interact.ChildComment;

@Data
@Schema(description = "子评论列表响应DTO - 游标分页")
public class ChildCommentCursorDto {
    
    @Schema(description = "子评论列表")
    private List<ChildComment> records;
    
    @Schema(description = "下一页的游标值")
    private String nextCursor;
    
    @Schema(description = "是否还有更多子评论")
    private Boolean hasMore;
    
    @Schema(description = "子评论总数")
    private Integer total;
    
    public ChildCommentCursorDto(List<ChildComment> records, String nextCursor, Boolean hasMore, Integer total) {
        this.records = records;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.total = total;
    }
} 