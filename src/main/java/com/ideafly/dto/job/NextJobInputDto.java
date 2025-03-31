package com.ideafly.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class NextJobInputDto {
    @NotNull(message = "当前职位ID不能为空")
    private Integer currentJobId = 1;
    @NotNull(message = "方向不能为空")
    @Schema(name = "direction", description = "-1: 上一条, 1: 下一条")
    private Integer direction;
}