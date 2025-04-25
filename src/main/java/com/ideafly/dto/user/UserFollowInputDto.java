package com.ideafly.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * 用户关注输入DTO
 */
@Data
@ToString
@Schema(description = "用户关注请求参数")
public class UserFollowInputDto {
    
    @Schema(description = "被关注者用户ID", required = true)
    @JsonProperty(value = "followedId", required = false)
    private Integer followedId;
    
    @JsonProperty(value = "followed_id", required = false)
    public void setFollowedIdFromSnakeCase(Integer followedId) {
        this.followedId = followedId;
    }
} 