package com.ideafly.dto.auth;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Telegram认证数据传输对象
 */
@Data
public class TelegramAuthDto {

    /**
     * Telegram用户ID
     */
    @NotBlank(message = "Telegram用户ID不能为空")
    private String id;

    /**
     * 用户名
     */
    @NotBlank(message = "名字不能为空")
    private String firstName;

    /**
     * 姓氏（可选）
     */
    private String lastName;

    /**
     * Telegram用户名（可选）
     */
    private String username;

    /**
     * 用户头像URL（可选）
     */
    private String photoUrl;

    /**
     * Telegram认证时间戳
     */
    @NotBlank(message = "认证时间不能为空")
    private String authDate;

    /**
     * 数据哈希值
     */
    @NotBlank(message = "哈希值不能为空")
    private String hash;
}