package com.bqsummer.framework.http;

import com.bqsummer.framework.exception.HttpClientException;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@Slf4j
public class HttpClientTemplate {

    public static final String CHARSET_UTF8 = "charset=UTF-8";

    public static final MediaType MEDIA_TYPE_JSON = okhttp3.MediaType.parse(APPLICATION_JSON_UTF8_VALUE);

    public static final MediaType MEDIA_TYPE_FORM = okhttp3.MediaType.parse(APPLICATION_FORM_URLENCODED_VALUE + ";" + CHARSET_UTF8);

    @Autowired
    private OkHttpClient okHttpClient;

    private volatile long deprecatedCallCount = 0L;

    /**
     * 统计get请求
     *
     * @param uri         绝对地址， http://或https://开头
     * @param parameters  请求参数
     * @param httpHeaders http头信息
     * @param contentType 默认传：HttpMediaTypeConstant.MEDIA_TYPE_FORM
     * @return 响应文本
     */
    public String doGet(String uri, Map<String, String> parameters, Map<String, String> httpHeaders, MediaType contentType) {
        String newUrl = HttpClientHelper.buildUri(uri, parameters).toString();
        Request okRequest = new Request.Builder().headers(HttpClientHelper.createHeaders(contentType, httpHeaders)).url(newUrl).get().build();
        return execute(okRequest);
    }

    public String doGet(String uri, Map<String, String> parameters, MediaType contentType) {
        String newUrl = HttpClientHelper.buildUri(uri, parameters).toString();
        Request okRequest = new Request.Builder().headers(HttpClientHelper.createHeaders(contentType, Maps.newHashMap())).url(newUrl).get().build();
        return execute(okRequest);
    }

    public String doGet(String uri, Map<String, String> parameters) {
        String newUrl = HttpClientHelper.buildUri(uri, parameters).toString();
        Request okRequest = new Request.Builder().headers(HttpClientHelper.createHeaders(MEDIA_TYPE_JSON, Maps.newHashMap())).url(newUrl).get().build();
        return execute(okRequest);
    }

    public String doGet(String uri) {
        String newUrl = HttpClientHelper.buildUri(uri, Maps.newHashMap()).toString();
        Request okRequest = new Request.Builder().headers(HttpClientHelper.createHeaders(MEDIA_TYPE_JSON, Maps.newHashMap())).url(newUrl).get().build();
        return execute(okRequest);
    }

    /**
     * 以form表单形式提交post请求
     *
     * @param uri         绝对地址， http://或https://开头
     * @param formMap     请求参数
     * @param httpHeaders http头信息
     * @param contentType 默认值：HttpMediaTypeConstant.MEDIA_TYPE_FORM
     * @return 响应文本
     */
    public String doPostForm(String uri, Map<String, String> formMap, Map<String, String> httpHeaders, MediaType contentType) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        if (contentType == null) {
            contentType = MEDIA_TYPE_FORM;
        }
        FormBody.Builder builder = HttpClientHelper.createFormBody(formMap, contentType);
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(contentType, httpHeaders))
                .post(builder.build())
                .build();
        return execute(okRequest);
    }


    /**
     * 以json形式提交post请求
     *
     * @param uri             绝对地址， http://或https://开头
     * @param requestBodyJson 请求json文本
     * @param httpHeaders     请求头信息
     * @param contentType     默认值：HttpMediaTypeConstant.MEDIA_TYPE_JSON
     * @return 响应文本
     */
    public String doPostJson(String uri, String requestBodyJson, Map<String, String> httpHeaders, MediaType contentType) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(contentType, httpHeaders))
                .post(RequestBody.create(MEDIA_TYPE_JSON, requestBodyJson == null ? "" : requestBodyJson))
                .build();

        return execute(okRequest);
    }

    public String doPostJson(String uri, String requestBodyJson, Map<String, String> httpHeaders) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(MEDIA_TYPE_JSON, httpHeaders))
                .post(RequestBody.create(MEDIA_TYPE_JSON, requestBodyJson == null ? "" : requestBodyJson))
                .build();

        return execute(okRequest);
    }

    public String doPostJson(String uri, String requestBodyJson) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(MEDIA_TYPE_JSON, Maps.newHashMap()))
                .post(RequestBody.create(MEDIA_TYPE_JSON, requestBodyJson == null ? "" : requestBodyJson))
                .build();

        return execute(okRequest);
    }


    /**
     * 以form表单形式提交Put请求
     *
     * @param uri         绝对地址， http://或https://开头
     * @param formMap     请求参数
     * @param httpHeaders 请求头信息
     * @param contentType 默认值：HttpMediaTypeConstant.MEDIA_TYPE_FORM
     * @return 响应文本
     */
    public String doPutForm(String uri, Map<String, String> formMap, Map<String, String> httpHeaders, MediaType contentType) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        FormBody.Builder builder = HttpClientHelper.createFormBody(formMap, contentType);
        if (contentType == null) {
            contentType = MEDIA_TYPE_FORM;
        }
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(contentType, httpHeaders))
                .put(builder.build())
                .build();

        return execute(okRequest);
    }

    /**
     * 以json形式提交put请求
     *
     * @param uri             绝对地址， http://或https://开头
     * @param jsonRequestBody json请求体
     * @param httpHeaders     默认值：HttpMediaTypeConstant.MEDIA_TYPE_JSON
     * @return 响应文本
     */
    public String doPutJson(String uri, String jsonRequestBody, Map<String, String> httpHeaders) {
        return doPutJson(uri, jsonRequestBody, httpHeaders, MEDIA_TYPE_JSON);
    }

    /**
     * 以json形式提交Put请求
     *
     * @param uri             绝对地址， http://或https://开头
     * @param jsonRequestBody json请求体
     * @param httpHeaders     头信息
     * @param contentType     默认值：HttpMediaTypeConstant.MEDIA_TYPE_JSON
     * @return 响应文本
     */
    public String doPutJson(String uri, String jsonRequestBody, Map<String, String> httpHeaders, MediaType contentType) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(contentType, httpHeaders))
                .put(RequestBody.create(MEDIA_TYPE_JSON, jsonRequestBody == null ? "" : jsonRequestBody))
                .build();
        return execute(okRequest);
    }


    /**
     * 执行http Delete 请求
     *
     * @param uri     绝对地址， http://或https://开头
     * @param formMap 请求参数
     * @return 响应文本
     */
    public String doDeleteForm(String uri, Map<String, String> formMap) {
        return doDeleteForm(uri, formMap, null, MEDIA_TYPE_FORM);
    }

    /**
     * 执行http Delete 请求
     *
     * @param uri         绝对地址， http://或https://开头
     * @param formMap     请求参数
     * @param httpHeaders http请求头
     * @param contentType 默认值：HttpMediaTypeConstant.MEDIA_TYPE_FORM
     * @return 响应文本
     */
    public String doDeleteForm(String uri, Map<String, String> formMap, Map<String, String> httpHeaders, MediaType contentType) {
        String newurl = HttpClientHelper.buildUri(uri, formMap).toString();
        if (contentType == null) {
            contentType = MEDIA_TYPE_FORM;
        }
        FormBody.Builder builder = HttpClientHelper.createFormBody(null, contentType);
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(contentType, httpHeaders))
                .delete(builder.build())
                .build();

        return execute(okRequest);
    }

    /**
     * 执行http Delete 请求
     *
     * @param uri             绝对地址， http://或https://开头
     * @param jsonRequestBody 请求参数
     * @param httpHeaders     http请求头
     * @param contentType     默认值：HttpMediaTypeConstant.MEDIA_TYPE_JSON
     * @return 响应文本
     */
    public String doDeleteJson(String uri, String jsonRequestBody, Map<String, String> httpHeaders, MediaType contentType) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(contentType, httpHeaders))
                .delete(RequestBody.create(MEDIA_TYPE_JSON, jsonRequestBody == null ? "" : jsonRequestBody))
                .build();

        return execute(okRequest);
    }

    /**
     * 文件上传
     *
     * @param uri         绝对地址， http://或https://开头
     * @param formMap     请求参数
     * @param httpHeaders http头信息
     * @param fileMap     待上传的文件，key是上传文件变量名，value是文件集合
     * @return 响应文本
     */
    public String doUploadFile(String uri, Map<String, String> formMap, Map<String, String> httpHeaders, Map<String, Collection<File>> fileMap) {
        if (CollectionUtils.isEmpty(fileMap)) {
            throw new HttpClientException("上传的文件不能为空");
        }
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        MultipartBody.Builder builder = HttpClientHelper.createUploadBodyByFile(formMap, fileMap);
        return doUpload(newurl, httpHeaders, builder);
    }

    /**
     * 文件字节数组上传
     *
     * @param uri         绝对地址， http://或https://开头
     * @param formMap     请求参数
     * @param httpHeaders http头信息
     * @param fileMap     待上传的文件byte数组。 Map<变量名,Map<文件名,字节数组>>
     * @return 响应文本
     */
    public String doUploadFileByte(String uri, Map<String, String> formMap, Map<String, String> httpHeaders, Map<String, Map<String, byte[]>> fileMap) {
        if (CollectionUtils.isEmpty(fileMap)) {
            throw new HttpClientException("上传的文件不能为空");
        }
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        MultipartBody.Builder builder = HttpClientHelper.createUploadBody(formMap, fileMap);
        return doUpload(newurl, httpHeaders, builder);
    }


    /**
     * 执行http请求
     *
     * @param uri         绝对地址， http://或https://开头
     * @param contentType 请求内容类型，见HttpMediaTypeConstant
     * @param httpHeaders 请求头
     * @param method      请求方式:GET,POST,DELETE,PUT
     * @param content     请求内容
     * @return
     */
    public String execute(String uri, MediaType contentType, Map<String, String> httpHeaders, String method, String content) {
        String newurl = HttpClientHelper.buildUri(uri, null).toString();
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(contentType, httpHeaders))
                .method(method.toUpperCase(), RequestBody.create(contentType, content))
                .build();
        return execute(okRequest);
    }

    private String doUpload(String newurl, Map<String, String> httpHeaders, MultipartBody.Builder builder) {
        MultipartBody body = builder.build();
        Request okRequest = new Request.Builder()
                .url(newurl).headers(HttpClientHelper.createHeaders(body.contentType(), httpHeaders))
                .post(body)
                .build();
        return execute(okRequest);
    }

    private String execute(Request okRequest) {
        String url = okRequest.url().toString();
        Response response = null;
        try {
            Call call = this.okHttpClient.newCall(okRequest);
            response = call.execute();
            return HttpClientHelper.getEntityString(response, url);
        } catch (IOException e) {
            HttpClientException httpClientException = new HttpClientException("Http client call error.uri:" + url, e, url);
            String urlExcludeParamters = url;
            if (!StringUtils.isEmpty(url)) {
                urlExcludeParamters = url.split("\\?")[0];
            }
            httpClientException.setRootUrl(urlExcludeParamters);
            throw httpClientException;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                    log.error("Close httpResponse error.", e);
                }
            }
        }
    }


}
