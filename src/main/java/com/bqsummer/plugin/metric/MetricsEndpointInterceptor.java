package com.bqsummer.plugin.metric;

public interface MetricsEndpointInterceptor {

    AbstractMetricsExporter exporter();

    void preHandle();

    void postHandle();
}
