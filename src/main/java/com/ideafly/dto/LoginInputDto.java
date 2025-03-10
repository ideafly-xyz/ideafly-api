package com.ideafly.dto;

import lombok.Data;

@Data
public class LoginInputDto {
    private String phoneNumber;
    private String verificationCode;
}
