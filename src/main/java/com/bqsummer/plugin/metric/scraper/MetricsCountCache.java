package com.bqsummer.plugin.metric.scraper;

import com.bqsummer.plugin.metric.metric.Metric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MetricsCountCache {

    public static final String SPLIT = "#$@";

    private ConcurrentHashMap<String, Double> metricCache  = new ConcurrentHashMap<>();

    public Double getDelta(Metric metric) {
        if (metric.needCalDelta()) {
            try {
                String key = buildKey(metric);
                Double v = metricCache.get(key);
                if (v == null) {
                    synchronized (key.intern()) {
                        v = metricCache.get(key);
                        if (v == null) {
                            metricCache.put(key, Double.valueOf(metric.getMetricStr()));
                            return null;
                        } else {
                            double delta = Double.parseDouble(metric.getMetricStr()) - v;
                            metricCache.put(key, Double.valueOf(metric.getMetricStr()));
                            return delta;
                        }
                    }
                } else {
                    double delta = Double.parseDouble(metric.getMetricStr()) - v;
                    metricCache.put(key, Double.valueOf(metric.getMetricStr()));
                    return delta;
                }
            }catch (Exception e) {
                log.error("get prometheus count delta failed.", e);
            }
        }
        return null;
    }

    public String buildKey(Metric metric) {
        String tags = "";
        if (metric.getTags() != null) {
            tags = metric.getTags().stream().map(e -> e.getKey() + SPLIT + e.getValue()).collect(Collectors.joining(SPLIT));
        }
        return metric.getName() + SPLIT + tags;
    }
}
