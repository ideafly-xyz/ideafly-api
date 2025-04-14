package com.ideafly.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PageBaseInputDto {
    @Schema(description = "页码",name = "page_num")
    private Integer pageNum = 1;
    @Schema(description = "页容量",name = "page_size")
    private Integer pageSize = 10;
    @Schema(description = "排序字段",name = "order_column")
    private String orderColumn = "id";
    @Schema(description = "升序/降序 true=升序 false=降序")
    private Boolean asc = false;
}