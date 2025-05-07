package com.ideafly.dto.job;

import com.ideafly.dto.PageBaseInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "职位列表请求参数")
public class JobListInputDto extends PageBaseInputDto {
    
    @Schema(description = "最大游标，获取更早的内容（向左滑动）")
    private String maxCursor;
    
    @Schema(description = "最小游标，获取更新的内容（向右滑动）")
    private String minCursor;
    
    @Schema(description = "是否使用游标分页")
    private Boolean useCursor;
}