package com.bqsummer.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Data
@ConfigurationProperties(value = "com.bqsummer.httpclient")
@Component
public class OkhttpProperties {

    private Integer connectTimeout = 60;

    private Integer readTimeout = 60;

    private Integer writeTimeout = 120;

    private Boolean retryOnConnectionFailure = false;

    private ConnectionPool connectionPool = new ConnectionPool();

    public class ConnectionPool {
        private Integer maxIdleConnections = 20;
        private Long keepAliveDuration = 120L;
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        public Integer getMaxIdleConnections() {
            return maxIdleConnections;
        }

        public void setMaxIdleConnections(Integer maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
        }

        public Long getKeepAliveDuration() {
            return keepAliveDuration;
        }

        public void setKeepAliveDuration(Long keepAliveDuration) {
            this.keepAliveDuration = keepAliveDuration;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }
    }

}
