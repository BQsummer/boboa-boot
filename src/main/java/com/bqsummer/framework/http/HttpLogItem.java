package com.bqsummer.framework.http;

public enum HttpLogItem {
    requestUri,
    requestMethod,
    requestApi,
    requestContentType,
    requestId,
    requestParameters,
    requestHeaders,
    requestBody,
    requestTime,
    responseStatus,
    responseContentType,
    responseBody,
    responseInterval,
    remoteAddr,
    userAgent,
    connectTimeout,
    socketTimeout,
    exceptionMessage,
    traceId,
    spanId,
    guardMatchResource,
    guardBlockType,
    commonRequestParameters;

    private HttpLogItem() {
    }
}
