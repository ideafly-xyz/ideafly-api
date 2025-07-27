package com.ideafly.dto.interact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

import com.ideafly.model.interact.ParentComment;

@Data
@Schema(description = "评论列表响应DTO - 游标分页")
public class JobCommentCursorDto {
    
    @Schema(description = "评论列表")
    private List<ParentComment> records;
    
    @Schema(description = "下一页的游标值 (Base64编码的复合游标)")
    private String nextCursor;
    
    @Schema(description = "是否还有更多历史评论")
    private Boolean hasMore;
    
    public JobCommentCursorDto(List<ParentComment> records, String nextCursor, Boolean hasMore) {
        this.records = records;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }
} 