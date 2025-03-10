package com.ideafly.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class R<T> {
    private Integer code;      // 返回状态码
    private String message;   // 返回信息
    private T data;          // 返回数据

    public static <T> R<T> success(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }
    public static <T> R<T> error(String message) {
        return new R<>(ErrorCode.SYSTEM_ERROR.getCode(), message, null);
    }
    public static <T> R<T> error(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg(), null);
    }
    public static <T> R<T> error(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(msg);
        r.setData(null);
        return r;
    }
}
