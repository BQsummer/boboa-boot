package com.bqsummer.configuration;


import com.bqsummer.framework.http.HttpClientTemplate;
import feign.Feign;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author hx
 */
@Configuration
@ConditionalOnClass(Feign.class)
@AutoConfigureBefore(OkhttpProperties.class)
public class HttpClientConfiguration {

    private final OkhttpProperties properties;

    public HttpClientConfiguration(OkhttpProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(properties.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(properties.getWriteTimeout(), TimeUnit.SECONDS)
                .retryOnConnectionFailure(properties.getRetryOnConnectionFailure())
                .connectionPool(new ConnectionPool(properties.getConnectionPool().getMaxIdleConnections(), properties.getConnectionPool().getKeepAliveDuration(), properties.getConnectionPool().getTimeUnit()))
//                .addInterceptor(new OkHttpInterceptor(properties))
                .build();
    }

    @Bean
    public HttpClientTemplate getHttpClientTemplate() {
        return new HttpClientTemplate();
    }

}
