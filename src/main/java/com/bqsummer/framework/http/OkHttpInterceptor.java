package com.bqsummer.framework.http;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OkHttpInterceptor
//        implements Interceptor
{

//    private OkHttpInterceptor() {
//    }
//
//    public OkHttpInterceptor(OkhttpProperties okhttpProperties) {
//        this.okhttpProperties = okhttpProperties;
//    }
//
//    private static final String TOO_LONG = "<Content Too Long, Omitted in Log>";
//
//    public static final Set<String> UNPRINTABLE_PARAMS = Sets.newHashSet("p_s", "p_t");
//
//    private static final Integer MAX_LENGTH = 30 * 1000;
//
//    private OkhttpProperties okhttpProperties;
//
//    @Override
//    public Response intercept(Chain chain) throws IOException {
//        long requestTime = 0;
//        long interval = 0;
//        String exceptionMessage = null;
//        StringBuilder entityString = new StringBuilder();
//        Response response = null;
//        Request request = chain.request();
//
//        String url = "";
//        try {
//            requestTime = System.currentTimeMillis();
//            chain = chain.withConnectTimeout(getConnectTimeOut(), TimeUnit.MILLISECONDS)
//                    .withReadTimeout(getReadTimeOut(), TimeUnit.MILLISECONDS);
//            response = chain.proceed(request);
//            interval = System.currentTimeMillis() - requestTime;
//            getResponseString(response, entityString);
//            return response;
//        } catch (IOException ex) {
//            HttpClientException httpClientException = new HttpClientException("Http client call error:" + ex
//                    .getMessage() + " .uri:" + url, ex, url);
//            exceptionMessage = ex.getMessage();
//            String urlExcludeParamters = url;
//            if (!StringUtils.isEmpty(url)) {
//                urlExcludeParamters = url.split("\\?")[0];
//            }
//            httpClientException.setRootUrl(urlExcludeParamters);
//            HttpClientThreadLocal.setRootUrl(urlExcludeParamters);
//            throw httpClientException;
//        } finally {
//            printLog(request, response, null, entityString.toString(), requestTime, interval,
//                    chain.connectTimeoutMillis(), chain.readTimeoutMillis(), exceptionMessage);
//        }
//    }
//
//    private void printLog(Request httpRequest, Response response, String requestBody,
//                          String entityString, long requestTime, long interval,
//                          int curConnectTimeout, int curSocketTimeout, String exceptionMessage) {
//
//        Map<HttpLogItem, Object> logMap = getLogMap(httpRequest, response, requestBody, entityString, requestTime,
//                interval, curConnectTimeout, curSocketTimeout, exceptionMessage);
//        LogControl.filterByLogControl(HttpClientThreadLocal.getLogControlConfig(), logMap);
//        filterByLength(logMap);
//        try {
//            putTraceIntoMDC();
////            log.info("[HttpOut]: " + JSON.toJSONString(logMap));
//        } finally {
//            removeTraceMDC();
//        }
//    }
//
//    private int getConnectTimeOut() {
//        return HttpClientThreadLocal.getConnectTimeout() == null ? okhttpProperties.getConnectTimeout() * 1000 : HttpClientThreadLocal.getConnectTimeout();
//    }
//
//    private int getReadTimeOut() {
//        return HttpClientThreadLocal.getReadTimeout() == null ? okhttpProperties.getReadTimeout() * 1000 : HttpClientThreadLocal.getReadTimeout();
//    }
//
//    public static boolean filterByContentType(String contentType) {
//        if (contentType == null) {
//            return false;
//        } else {
//            return contentType.toLowerCase().contains("image") || contentType.toLowerCase().contains("multipart") || contentType.toLowerCase().contains("javascript") || contentType.toLowerCase().contains("css");
//        }
//    }
//
//    private void getResponseString(Response response, StringBuilder stringBuilder) throws IOException {
//        if (response.body() == null) {
//            return;
//        }
//        MediaType responseContentType = response.body().contentType();
//        if (responseContentType != null && filterByContentType(responseContentType.type())) {
//            stringBuilder.append("<filterByContentType>");
//            return;
//        }
//        buildResponseString(response, stringBuilder);
//    }
//
//    private String buildResponseString(Response response, StringBuilder stringBuilder) throws IOException {
//        int statusCode = response.code();
//        String url = response.request().url().toString();
//        ResponseBody responseCopy = response.peekBody(Long.MAX_VALUE);
//        String entityString = responseCopy.string();
//        stringBuilder.append(entityString);
//        HttpClientThreadLocal.setRootUrl(url);
//
//        if (statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_BAD_REQUEST) {
//            return stringBuilder.toString();
//        } else {
//            return buildErrorResponse(url, entityString, statusCode);
//        }
//    }
//
//    private String buildErrorResponse(String url, String entityString, int statusCode) {
//        ErrorResponse errorResponse;
//        try {
//            if (!StringUtils.isEmpty(entityString)) {
//                errorResponse = JSON.parseObject(entityString, ErrorResponse.class);
//                if(errorResponse.getStatus()==0){
//                    errorResponse.setStatus(statusCode);
//                }
//                if(errorResponse.getCode()==0){
//                    errorResponse.setCode(INTERNAL_CALL_ERROR_CODE);
//                }
//                if(StringUtils.isEmpty(errorResponse.getDeveloperMessage())){
//                    errorResponse.setDeveloperMessage(entityString);
//                }
//                if(StringUtils.isEmpty(errorResponse.getMessage())){
//                    errorResponse.setMessage(INTERNAL_CALL_ERROR_MESSAGE);
//                }
//            } else {
//                errorResponse = new ErrorResponse(statusCode, INTERNAL_CALL_ERROR_CODE, url, INTERNAL_CALL_ERROR_MESSAGE);
//            }
//        } catch (Exception e) {
//            errorResponse = new ErrorResponse(statusCode, INTERNAL_CALL_ERROR_CODE, url, entityString, INTERNAL_CALL_ERROR_MESSAGE);
//            throw new HttpClientException(statusCode, errorResponse, url, entityString);
//        }
//        throw new HttpClientException(statusCode, errorResponse, url, entityString);
//    }
//
//    private Map<HttpLogItem, Object> getLogMap(Request httpRequest, Response response,
//                                               String requestBody, String entityString, long requestTime, long interval,
//                                               int curConnectTimeout, int curSocketTimeout, String exceptionMessage) {
//        Map<HttpLogItem, Object> logMap = new LinkedHashMap<>();
//        logMap.put(HttpLogItem.requestUri, getPrintableRequestUri(httpRequest.url().toString()));
//        logMap.put(HttpLogItem.requestMethod, httpRequest.method());
//        logMap.put(HttpLogItem.requestHeaders, httpRequest.headers());
//
//        String requestContentType = httpRequest.header(HttpHeaders.CONTENT_TYPE);
//        if (!StringUtils.isEmpty(requestContentType)) {
//            logMap.put(HttpLogItem.requestContentType, requestContentType);
//        }
//
//        logMap.put(HttpLogItem.requestParameters, getPrintableRequestParameters());
//        logMap.put(HttpLogItem.commonRequestParameters, HttpClientThreadLocal.getPrintableCommonRequestParameters());
//        if (!filterByContentType(requestContentType)) {
//            logMap.put(HttpLogItem.requestBody, requestBody);
//        }
//
//        logMap.put(HttpLogItem.requestTime, new Date(requestTime));
//
//        if (response != null) {
//            logMap.put(HttpLogItem.responseStatus, response.code());
//            String responseContentType = null;
//            ResponseBody body = response.body();
//            if (body != null && body.contentType() != null) {
//                responseContentType = body.contentType().toString();
//                logMap.put(HttpLogItem.responseContentType, responseContentType);
//            }
//            if (!filterByContentType(responseContentType)) {
//                logMap.put(HttpLogItem.responseBody, entityString);
//            }
//            logMap.put(HttpLogItem.responseInterval, interval);
//        } else {
//            logMap.put(HttpLogItem.connectTimeout, curConnectTimeout);
//            logMap.put(HttpLogItem.socketTimeout, curSocketTimeout);
//            logMap.put(HttpLogItem.exceptionMessage, exceptionMessage);
//        }
//        return logMap;
//    }
//
//    public static String getPrintableRequestUri(String originalRequestUri) {
//        if (org.springframework.util.StringUtils.isEmpty(originalRequestUri)) {
//            return "";
//        } else {
//            String[] urlTokens = org.springframework.util.StringUtils.split(originalRequestUri, "?");
//            if (urlTokens != null && urlTokens.length == 2 && !Objects.equals(urlTokens[1], "")) {
//                String uri = urlTokens[0];
//                String queryString = urlTokens[1];
//                Map<String, String> requestParams = convertQueryStringToParamsMap(queryString);
//                requestParams = getPrintableRequestParameters(requestParams);
//                return !CollectionUtils.isEmpty(requestParams) ? uri + "?" + convertParamMapToQueryString(requestParams) : uri;
//            } else {
//                return originalRequestUri;
//            }
//        }
//    }
//
//    private static String convertParamMapToQueryString(Map<String, String> requestParams) {
//        return requestParams.entrySet().stream()
//                .map(e -> e.getKey() + "=" + e.getValue())
//                .collect(Collectors.joining("&"));
//    }
//
//    public static Map<String, String> convertQueryStringToParamsMap(String queryString) {
//        Map<String, String> requestParams = new TreeMap<>();
//        String[] keyValuePair;
//        if (Strings.isBlank(queryString)) {
//            return requestParams;
//        } else {
//            keyValuePair = queryString.split("&");
//        }
//        String[] keyValueToken;
//        String key;
//        String value;
//        for (String keyValue : keyValuePair) {
//            keyValueToken = org.springframework.util.StringUtils.split(keyValue, "=");
//            if (keyValueToken != null && keyValueToken.length == 2) {
//                key = keyValueToken[0];
//                value = keyValueToken[1];
//                requestParams.put(key, value);
//            }
//        }
//        return requestParams;
//    }
//
//    public static void filterByLength(Map<HttpLogItem, Object> logMap) {
//        Object requestParameters = logMap.get(HttpLogItem.requestParameters);
//        if (requestParameters != null) {
//            String requestParameterString = JSON.toJSONString(requestParameters);
//            if (requestParameterString.getBytes().length > MAX_LENGTH) {
//                logMap.put(HttpLogItem.requestParameters, TOO_LONG);
//            }
//        }
//
//        Object requestHeaers = logMap.get(HttpLogItem.requestHeaders);
//        if (requestParameters != null) {
//            String requestHeaderString = JSON.toJSONString(requestHeaers);
//            if (requestHeaderString.getBytes().length > MAX_LENGTH) {
//                logMap.put(HttpLogItem.requestHeaders, TOO_LONG);
//            }
//        }
//
//        String requestBody = (String) logMap.get(HttpLogItem.requestBody);
//        if (requestBody != null && requestBody.getBytes().length > MAX_LENGTH) {
//            logMap.put(HttpLogItem.requestBody, TOO_LONG);
//        }
//
//        String responseBody = (String) logMap.get(HttpLogItem.responseBody);
//        if (responseBody != null && responseBody.getBytes().length > MAX_LENGTH) {
//            logMap.put(HttpLogItem.responseBody, TOO_LONG);
//        }
//    }
//
//    private void putTraceIntoMDC() {
//        if (!StringUtils.isEmpty(HttpClientThreadLocal.getTraceId()) && StringUtils.isEmpty(MDC.get("traceId"))) {
//            MDC.put("traceId", HttpClientThreadLocal.getTraceId());
//        }
//        MDC.put("logType", LogType.HttpOut.name());
//    }

//    private void removeTraceMDC() {
//        MDC.remove("logType");
//    }
}
