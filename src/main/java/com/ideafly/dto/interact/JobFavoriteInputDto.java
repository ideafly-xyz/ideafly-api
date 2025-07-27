package com.ideafly.dto.interact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JobFavoriteInputDto {
    @NotNull(message = "jobId is required")
    @Schema(description = "job id",name = "job_id")
    private Integer jobId;
    @NotNull(message = "isFavorite is required")
    @Schema(description = "1: favorite, 0: not favorite",name = "is_favorite")
    private Integer isFavorite;
}
