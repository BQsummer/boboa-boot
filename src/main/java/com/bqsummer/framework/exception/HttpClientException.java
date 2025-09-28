package com.bqsummer.framework.exception;

import com.alibaba.fastjson2.JSON;
import com.bqsummer.common.vo.Response;
import com.google.common.collect.Maps;
import org.springframework.util.StringUtils;

import java.util.Map;

public class HttpClientException extends RuntimeException{

    private static final long serialVersionUID = 7753483928731646476L;

    private Integer httpStatusCode;

    private Response errorResponse;

    private String url;

    private String errorBody;

    private String rootUrl;

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public HttpClientException(Integer httpStatusCode, Response errorResponse) {
        this(httpStatusCode, errorResponse, null);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(Integer httpStatusCode, Response errorResponse, String url, String errorBody) {
        this(httpStatusCode, errorResponse, url);
        this.errorBody = errorBody;
    }

    public HttpClientException(Integer httpStatusCode, Response errorResponse, String url) {
        super(getMessage(null, errorResponse, url));
        this.httpStatusCode = httpStatusCode;
        this.errorResponse = errorResponse;
        this.url = url;
    }

    public HttpClientException(String message, Throwable cause, String url, String errorBody) {
        this(message, cause, url);
        this.errorBody = errorBody;
    }

    public HttpClientException(String message, Throwable cause, String url) {
        super(getMessage(message, null, url), cause);
        this.url = url;
    }

    public HttpClientException(String message, String url) {
        super(getMessage(message, null, url));
        this.url = url;
    }

    public String getErrorBody() {
        return errorBody;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public Response getErrorResponse() {
        return errorResponse;
    }

    private static String extractApi(String url) {
        if (StringUtils.isEmpty(url)) {
            return url;
        } else {
            int sepIndex = url.indexOf("?");
            if (sepIndex < 0) {
                return url;
            } else {
                return url.substring(0, sepIndex);
            }
        }
    }

    private static String getMessage(String message, Response errorResponse, String url) {
        Map<String, Object> messageMap = Maps.newHashMap();
        //messageMap.put("requestApi", StatisticsThreadLocal.getApiName());
        //messageMap.put("requestId", ParameterThreadLocal.getRequestId());
        messageMap.put("message", message);
        messageMap.put("httpOutUrl", extractApi(url));
        if (errorResponse != null) {
            messageMap.putAll(errorResponse.toMap());
        }
        return JSON.toJSONString(messageMap);
    }

    public String getUrl() {
        return extractApi(url);
    }

}
