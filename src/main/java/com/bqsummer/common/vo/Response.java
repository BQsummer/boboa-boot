package com.bqsummer.common.vo;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Response {

    public static final int SUCCESS_STATUS = 0;

    private int status;
    private int errCode;
    private String message;
    private String developerMessage;
    private String errorLevel;
    private String rootUrl;

    public Response(int status, int code, String rootUrl, String message) {
        this.status = status;
        this.rootUrl = rootUrl;
        this.message = message;
    }

    public Response(int status, int code, String rootUrl, String developerMessage, String message) {
        this.status = status;
        this.rootUrl = rootUrl;
        this.developerMessage = developerMessage;
        this.message = message;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("status", status);
        map.put("message", message);
        map.put("developerMessage", developerMessage);
        map.put("errorLevel", errorLevel);
        map.put("rootUrl", rootUrl);
        return map;
    }

    public static Response success() {
        return Response.builder().errCode(SUCCESS_STATUS).build();
    }

    public static Response fail(Integer errCode, String errMsg) {
        return Response.builder().errCode(errCode).message(errMsg).build();
    }

    public static Response fail(Integer errCode, String errMsg, String developMessage) {
        return Response.builder().errCode(errCode).message(errMsg).developerMessage(developMessage).build();
    }
}
