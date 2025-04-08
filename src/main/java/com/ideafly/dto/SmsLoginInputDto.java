package com.ideafly.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SmsLoginInputDto {
    @Schema(description = "手机号",name = "phone_number")
    @NotBlank(message = "手机号不能为空")
    private String phoneNumber;
    @Schema(description = "验证码",name = "verification_code")
    @NotBlank(message = "验证码不能为空")
    private String verificationCode;
}
