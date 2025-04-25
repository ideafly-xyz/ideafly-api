package com.ideafly.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户关注输入DTO
 */
@Data
@Schema(description = "用户关注请求参数")
public class UserFollowInputDto {
    
    @Schema(description = "被关注者用户ID", required = true)
    private Integer followedId;
} 