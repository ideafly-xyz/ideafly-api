package com.ideafly.dto.job;

import com.ideafly.dto.PageBaseInputDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "获取职位列表请求")
public class JobListInputDto extends PageBaseInputDto {
    @Schema(description = "最大游标（用于获取比此更早的数据）")
    private String maxCursor;
    
    @Schema(description = "最小游标（用于获取比此更新的数据）")
    private String minCursor;
    
    @Schema(description = "页容量",name = "page_size")
    private Integer pageSize = 0;  // 默认每页4条记录，但会被Flutter传入的值覆盖
}