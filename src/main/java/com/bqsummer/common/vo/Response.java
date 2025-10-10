package com.bqsummer.common.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 通用响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    private int code;
    private String message;
    private T data;
    private boolean success;

    public static <T> Response<T> success() {
        return new Response<>(200, "success", null, true);
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(200, "success", data, true);
    }

    public static <T> Response<T> success(String message, T data) {
        return new Response<>(200, message, data, true);
    }

    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null, false);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(500, message, null, false);
    }

    /**
     * 适配异常处理所需的错误返回（包含额外明细）。
     * message 会与 detail 合并，保持 data 为 null，success 为 false。
     */
    public static <T> Response<T> fail(int code, String message, String detail) {
        String merged = (detail == null || detail.isBlank()) ? message : (message + ": " + detail);
        return new Response<>(code, merged, null, false);
    }

    /**
     * 适配仅包含 code 与 message 的失败返回；与 error 等价。
     */
    public static <T> Response<T> fail(int code, String message) {
        return error(code, message);
    }
}
