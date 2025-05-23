package com.ideafly.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    NO_AUTH(401, "没有权限"),
    TOKEN_EXPIRED(402, "Token已过期，请重新登录"),
    TOKEN_NULL(403, "刷新令牌不能为空"), // Add this new ErrorCode
    INVALID_TOKEN(404, "无效的令牌格式"),
    SYSTEM_ERROR(500, "系统似乎出现了点小问题"),
    PARAM_ERROR(101, "参数错误"),
    EXECUTE_SQL_ERROR(10001, "执行SQL异常"),
    DEVELOPING(10002, "系統正在紧急开发中，敬请期待~"),;
    private final int code;
    private final String msg;
    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}