package com.ideafly.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "刷新令牌请求DTO")
public class RefreshTokenDto {
    @Schema(description = "刷新令牌", name = "refreshToken")
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
