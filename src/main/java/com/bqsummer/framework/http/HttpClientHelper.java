package com.bqsummer.framework.http;

import com.alibaba.fastjson2.JSON;
import com.bqsummer.framework.exception.HttpClientException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import okhttp3.*;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import static org.springframework.cloud.openfeign.encoding.HttpEncoding.CONTENT_TYPE;

public class HttpClientHelper {

    public static final okhttp3.MediaType MEDIA_TYPE_MULTIPART_FORM_DATA = okhttp3.MediaType.parse("multipart/form-data; charset=iso-8859-1");

    public static Headers createHeaders(okhttp3.MediaType mediaType, Map<String, String> headersMap) {
        Map<String, String> newHeadersMap = Maps.newHashMap();
        newHeadersMap.put(CONTENT_TYPE, mediaType == null ? MediaType.APPLICATION_JSON_UTF8_VALUE : mediaType.toString());
        if (!CollectionUtils.isEmpty(headersMap)) {
            newHeadersMap.putAll(headersMap);
        }
        return Headers.of(newHeadersMap);
    }


    /**
     * 根据parameters参数，重新构建URI
     *
     * @param uriStr
     * @param parameters
     * @return
     */
    public static URI buildUri(String uriStr, Map<String, String> parameters) {
        Map<String, Collection<String>> stringCollectionMap = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(parameters)) {
            for (Map.Entry<String, String> parameter : parameters.entrySet()) {
                stringCollectionMap.put(parameter.getKey(), Lists.newArrayList(parameter.getValue()));
            }
        }
        return buildUriWithParameters(uriStr, stringCollectionMap);
    }

    public static URI buildUriWithParameters(String uriStr, Map<String, Collection<String>> parameters) {
        try {
            URIBuilder uriBuilder = new URIBuilder(new URI(uriStr));
            if (parameters != null && parameters.size() > 0) {
                for (Map.Entry<String, Collection<String>> parameter : parameters.entrySet()) {
                    Collection<String> pValues = parameter.getValue();
                    for (String value : pValues) {
                        uriBuilder.addParameter(parameter.getKey(), value);
//                        HttpClientThreadLocal.getRequestParameters().put(parameter.getKey(), value);
                    }
                }
            }
            return uriBuilder.build();
        } catch (URISyntaxException ex) {
            HttpClientException httpClientException = new HttpClientException("Http client forward error.uri:" + uriStr, ex, uriStr);
            httpClientException.setRootUrl(uriStr);
            throw httpClientException;
        }
    }

    public static String getEntityString(Response response, String url) {
        try {
            int statusCode = response.code();
            String entityString = null;
            if (response.body() != null) {
                entityString = response.body().string();
            }

            if (statusCode >= 200 && statusCode < 400) {
                return entityString;
            } else {
                com.bqsummer.common.vo.Response errorResponse = null;
                try {
                    if (!StringUtils.isEmpty(entityString)) {
                        errorResponse = JSON.parseObject(entityString, com.bqsummer.common.vo.Response.class);
                    }
                } catch (Exception e) {
                    throw new HttpClientException(entityString, e, url, entityString);
                }
                throw new HttpClientException(statusCode, errorResponse, url, entityString);
            }
        } catch (IOException e) {
            throw new HttpClientException("Parse http response error", e, url);
        }
    }


    /**
     * 递归查找Throwable对象中包含的HttpClientException
     *
     * @param
     * @return
     */
    public static HttpClientException findHttpClientException(Throwable t) {
        if (t == null) {
            return null;
        }
        if (t instanceof HttpClientException) {
            return (HttpClientException) t;
        }
        return findHttpClientException(t.getCause());
    }

    public static FormBody.Builder createFormBody(Map<String, String> formMap, okhttp3.MediaType mediaType) {
        FormBody.Builder builder = new FormBody.Builder(mediaType == null ? null : mediaType.charset());
        if (!CollectionUtils.isEmpty(formMap)) {
            for (Map.Entry<String, String> formItem : formMap.entrySet()) {
                builder.add(formItem.getKey(), formItem.getValue());
            }
        }
        return builder;
    }

    /**
     * 创建上传请求body
     *
     * @param formMap form字段值
     * @param fileMap form文件:key是参数值，文件名是file
     * @return
     */
    public static MultipartBody.Builder createUploadBodyByFile(Map<String, String> formMap, Map<String, Collection<File>> fileMap) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        for (Map.Entry<String, String> formItem : formMap.entrySet()) {
            builder.addFormDataPart(formItem.getKey(), formItem.getValue());
        }

        for (Map.Entry<String, Collection<File>> fileItem : fileMap.entrySet()) {
            Collection<File> files = fileItem.getValue();
            if (CollectionUtils.isEmpty(files)) {
                throw new HttpClientException("上传文件不能为空");
            }
            for (File file : files) {
                if (!file.exists()) {
                    throw new HttpClientException("上传文件不存在");
                }
                builder.addFormDataPart(fileItem.getKey(), file.getName(), RequestBody.create(MEDIA_TYPE_MULTIPART_FORM_DATA, file));
            }
        }
        return builder;
    }

    /**
     * 创建上传请求body
     *
     * @param formMap form字段值
     * @param fileMap 文件字节数组map
     * @return
     */
    public static MultipartBody.Builder createUploadBody(Map<String, String> formMap, Map<String, Map<String, byte[]>> fileMap) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        for (Map.Entry<String, String> formItem : formMap.entrySet()) {
            builder.addFormDataPart(formItem.getKey(), formItem.getValue());
        }

        for (Map.Entry<String, Map<String, byte[]>> fileItem : fileMap.entrySet()) {
            Map<String, byte[]> files = fileItem.getValue();
            if (CollectionUtils.isEmpty(files)) {
                throw new HttpClientException("上传文件不能为空");
            }
            for (Map.Entry<String, byte[]> filebytes : files.entrySet()) {
                if (filebytes.getValue() == null) {
                    throw new HttpClientException("上传文件不能为空");
                }
                builder.addFormDataPart(fileItem.getKey(), filebytes.getKey(), RequestBody.create(MEDIA_TYPE_MULTIPART_FORM_DATA, filebytes.getValue()));
            }
        }
        return builder;
    }


}
