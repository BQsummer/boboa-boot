package com.bqsummer.framework.exception;

public class GlobalErrorResponseConstants {
    public static final int COMMON_SERVER_ERROR_CODE = 1001;
    public static final String COMMON_SERVER_ERROR_MESSAGE = "系统忙不过来啦，稍等一下";
    public static final int COMMON_CLIENT_ERROR_CODE = 1005;
    public static final String COMMON_CLIENT_ERROR_MESSAGE = "您的操作有误，重新试试吧";
    public static final int REQUEST_ERROR_CODE = 1002;
    public static final String REQUEST_ERROR_MESSAGE = "输入信息有误，重新试试吧";
    public static final int COMMON_BIZ_ERROR_CODE = 1003;
    public static final int INTERNAL_CALL_ERROR_CODE = 1004;
    public static final String INTERNAL_CALL_ERROR_MESSAGE = "系统忙不过来啦，稍等一下";
    public static final int SESSION_TIMEOUT_ERROR_CODE = 1006;
    public static final String SESSION_TIMEOUT_ERROR_MESSAGE = "发呆的时间太长，请先登录哦";
    public static final int SHORT_SESSION_TIMEOUT_ERROR_CODE = 1007;
    public static final String SHORT_SESSION_TIMEOUT_ERROR_MESSAGE = "session已过期，请重新获取";
    public static final int PERMISSION_DENIED_ERROR_CODE = 1008;
    public static final String PERMISSION_DENIED_ERROR_MESSAGE = "您没有权限进行该操作";
    public static final int AUTHENTICATION_FAILED_ERROR_CODE = 1009;
    public static final String AUTHENTICATION_FAILED_ERROR_MESSAGE = "认证失败，用户名或密码错误";
    public static final int RATE_LIMITED_ERROR_CODE = 1010;
    public static final String RATE_LIMITED_ERROR_MESSAGE = "请求过于频繁，请稍后再试";

    public GlobalErrorResponseConstants() {
    }
}
