package com.ideafly.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SmsLoginInputDto {
    @NotBlank(message = "手机号不能为空")
    private String phoneNumber;
    @NotBlank(message = "验证码不能为空")
    private String verificationCode;
}
