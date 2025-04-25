package com.ideafly.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户关注统计DTO
 */
@Data
@Schema(description = "用户关注统计响应")
public class UserFollowStatsDto {
    
    @Schema(description = "用户ID", required = true)
    private Integer userId;
    
    @Schema(description = "用户名", required = true)
    private String username;
    
    @Schema(description = "粉丝数量(关注该用户的人数)", required = true)
    private Integer followersCount;
    
    @Schema(description = "关注数量(该用户关注的人数)", required = true)
    private Integer followingCount;
    
    @Schema(description = "互相关注数量", required = true)
    private Integer mutualFollowCount;
} 