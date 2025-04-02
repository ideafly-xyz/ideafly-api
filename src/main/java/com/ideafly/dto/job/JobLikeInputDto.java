package com.ideafly.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JobLikeInputDto {
    @NotNull(message = "jobId is required")
    @Schema(description = "job id",name = "job_id")
    private Integer jobId;
    @NotNull(message = "是否点赞不能为空")
    @Schema(description = "1: 点赞, 0: 取消点赞",name = "is_like")
    private Integer isLike;
}
