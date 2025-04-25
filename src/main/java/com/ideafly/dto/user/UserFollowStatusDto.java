package com.ideafly.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户关注状态DTO
 */
@Data
@Schema(description = "用户关注状态响应")
public class UserFollowStatusDto {
    
    @Schema(description = "是否关注", required = true)
    private boolean isFollowing;
    
    @Schema(description = "被关注者用户ID", required = true)
    private Integer followedId;
    
    @Schema(description = "关注者用户ID", required = true)
    private Integer followerId;
} 