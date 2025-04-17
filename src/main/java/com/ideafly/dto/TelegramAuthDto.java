package com.ideafly.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TelegramAuthDto {
    @NotNull(message = "id不能为空")
    private String id;
    
    @NotNull(message = "first_name不能为空")
    private String firstName;
    
    private String lastName;
    private String username;
    private String photoUrl;
    
    @NotNull(message = "auth_date不能为空")
    private Long authDate;
    
    @NotNull(message = "hash不能为空")
    private String hash;
} 