package com.ideafly.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TelegramAuthDto {
    @NotNull(message = "id不能为空")
    private String id;
    
    @NotNull(message = "first_name不能为空")
    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("username")
    private String username;

    @JsonProperty("photo_url")
    private String photoUrl;
    
    @NotNull(message = "auth_date不能为空")
    @JsonProperty("auth_date")
    private Long authDate;
    
    @NotNull(message = "hash不能为空")
    @JsonProperty("hash")
    private String hash;
} 