package com.ideafly.dto;

import com.ideafly.dto.user.UserDto;
import lombok.Data;

/**
 * @author rfs
 * @date 2025/03/11
 */
@Data
public class LoginSuccessOutputDto {
    private String accessToken;
    private String refreshToken;
    private UserDto userInfo;
}
