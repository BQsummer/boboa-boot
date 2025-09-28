package com.bqsummer.plugin.metric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AbstractMetricsEndpoint {

    public static final String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    @Autowired
    protected ApplicationContext applicationContext;

    protected AbstractMetricsExporter metrics;

    @ReadOperation(produces = CONTENT_TYPE_004)
    public String scrape() {
        Map<String, MetricsEndpointInterceptor> interceptorMap = applicationContext
                .getBeansOfType(MetricsEndpointInterceptor.class);

        // 确保执行顺序是稳定的
        Map<String, MetricsEndpointInterceptor> treeMap = new TreeMap<>(interceptorMap);

        // 处理拦截器
        List<MetricsEndpointInterceptor> list = new ArrayList();
        for (Map.Entry<String, MetricsEndpointInterceptor> entry : treeMap.entrySet()) {
            MetricsEndpointInterceptor v = entry.getValue();
            if (metrics == v.exporter()) {
                list.add(v);
                v.preHandle();
            }
        }

        String ret = metrics.scrape();

        // 处理拦截器
        for (int i = list.size(); i > 0; i--) {
            list.get(i - 1).postHandle();
        }

        return ret;
    }

}
